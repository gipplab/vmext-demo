package org.citeplag.controller;

import com.formulasearchengine.mathmltools.converters.LaTeXMLConverter;
import com.formulasearchengine.mathmltools.converters.MathoidConverter;
import com.formulasearchengine.mathmltools.converters.mathoid.MathoidEndpoints;
import com.formulasearchengine.mathmltools.converters.mathoid.MathoidInfoResponse;
import com.formulasearchengine.mathmltools.converters.mathoid.MathoidTypes;
import com.formulasearchengine.mathmltools.io.XmlDocumentWriter;
import com.google.common.base.Charsets;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.config.LaTeXMLRemoteConfig;
import org.citeplag.config.MathoidConfig;
import org.citeplag.util.MMLEndpointCache;
import org.citeplag.beans.MathoidRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Andre Greiner-Petter
 */
@RestController
@RequestMapping("/v1/media/math")
public class MediaController {

    private static final Logger LOG = LogManager.getLogger(MediaController.class.getName());

    // Header entry to provide hash values in the response
    private static final String HEADER_HASH_KEY = "x-resource-location";

    // Caches for Mathoid Requests
    private static Map<String, String> requestHashesTable = new ConcurrentHashMap<>(new HashMap<>());
    private static Map<String, MMLEndpointCache> responseHashesTable = new ConcurrentHashMap<>(new HashMap<>());

    @Autowired
    private MathoidConfig mathoidConfig;

    @Autowired
    private LaTeXMLRemoteConfig laTeXMLRemoteConfig;

    private static HttpHeaders buildResponseHeader(String hash) {
        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON_UTF8);
        header.set(HEADER_HASH_KEY, hash);
        return header;
    }

    private static MathoidEndpoints getEndpoint(String type) {
        switch (type) {
            case "svg":
                return MathoidEndpoints.SVG_ENDPOINT;
            case "mml":
                return MathoidEndpoints.MML_ENDPOINT;
            case "png":
                return MathoidEndpoints.PNG_ENDPOINT;
            default:
                return null;
        }
    }

    @PostMapping("/check/{type}")
    @ApiOperation(value = "Checks the supplied TeX formula for correctness and returns the normalised formula representation as well as information about identifiers.")
    public HttpEntity<MathoidInfoResponse> mathoidCheck(
            @PathVariable
            @ApiParam(
                    allowableValues = "tex, inline-tex, chem, pmml, ascii",
                    value = "The input type of the given formula; can be tex or inline-tex",
                    defaultValue = "tex",
                    required = true)
                    String type,
            @RequestParam
            @ApiParam(
                    name = "q",
                    value = "The formula to check",
                    required = true)
                    String q
    ) throws IOException, javax.xml.transform.TransformerException { // TODO we still ignore the request type!
        // check if the request has already been made
        MathoidRequest request = new MathoidRequest(q, type);
        String reqHash = request.sha1Hash();

        String knownHash = requestHashesTable.get(reqHash);
        // the hash is known -> there must be an object
        if (knownHash != null) {
            MMLEndpointCache cacheObj = responseHashesTable.get(knownHash);
            // if this object is null -> the cache is broken
            MathoidInfoResponse resp = cacheObj.getCorrespondingResponse();
            // knownHash is the hash of the result -> put it to the headre
            HttpHeaders header = buildResponseHeader(knownHash);
            return new HttpEntity<>(resp, header);
        }

        // the element is not in the cache -> send it to LaTeXML and create response
        // setup latexml
        LaTeXMLConverter latexml = new LaTeXMLConverter(laTeXMLRemoteConfig);
        latexml.semanticMode();
        Path p = Paths.get(laTeXMLRemoteConfig.getContentPath());
        latexml.redirectLatex(p);

        // parse requested expression
        Document mmlDoc = latexml.convertToDoc(q);
        String rawTex = mmlDoc.getDocumentElement().getAttribute("alttext");
        String mml = null;
        try {
            mml = XmlDocumentWriter.stringify(mmlDoc);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        if (rawTex == null || rawTex.isEmpty()) {
            rawTex = q; // if we cannot extract alttext -> take the original input
        }

        MathoidRequest cleanedRequest = new MathoidRequest(rawTex, type);
        String respHash = cleanedRequest.sha1Hash();

        MathoidInfoResponse mathoidResp = new MathoidInfoResponse();
        mathoidResp.setSuccess(true);
        mathoidResp.setChecked(rawTex);
        mathoidResp.setEndsWithDots(q.endsWith("\\s*\\.\\s*"));

        // create cache element
        MMLEndpointCache newCacheEntry = new MMLEndpointCache(cleanedRequest, mathoidResp);
        newCacheEntry.setMml(mml);

        // linking hashes and add element to hash tables
        requestHashesTable.put(reqHash, respHash);
        responseHashesTable.put(respHash, newCacheEntry);

        // send hash of answer back
        HttpHeaders header = buildResponseHeader(respHash);
        return new HttpEntity<>(mathoidResp, header);
    }

    @GetMapping("/formula/{hash}")
    @ApiOperation(value = "Returns the previously-stored formula via /check/{type} for the given hash.")
    public HttpEntity mathoidGetFormula(
            @PathVariable
            @ApiParam(
                    name = "hash",
                    value = "The input type of the given formula; can be tex or inline-tex",
                    required = true)
                    String hash
    ) {
        // try to get cache element from hash -> NullPointerException will be thrown -> Results in 404
        MMLEndpointCache cache = responseHashesTable.get(hash);

        if (cache == null) {
            return new ResponseEntity<>("Unknown Hash", HttpStatus.NOT_FOUND);
        }

        HttpHeaders header = buildResponseHeader(hash);
        return new HttpEntity<>(cache.getOriginalRequest(), header);
    }

    @GetMapping("/render/{format}/{hash}")
    @ApiOperation(value = "Given a request hash, renders a TeX formula into its mathematic representation in the given format.")
    public HttpEntity mathoidRender(
            @PathVariable
            @ApiParam(
                    allowableValues = "svg, mml, png",
                    name = "format",
                    value = "The output format; can be svg or mml",
                    required = true)
                    String format,
            @PathVariable
            @ApiParam(
                    name = "hash",
                    value = "The hash string of the previous POST data",
                    required = true)
                    String hash,
            HttpServletRequest request
    ) {
        MMLEndpointCache cacheEntry = responseHashesTable.get(hash);

        if (cacheEntry == null) {
            return new ResponseEntity<>("Unknown Hash", HttpStatus.NOT_FOUND);
        }

        HttpHeaders header = buildResponseHeader(hash);
        MathoidConverter converter = new MathoidConverter(mathoidConfig);
        String mml = cacheEntry.getMml();

        switch (format) {
            case "svg":
                String svg = cacheEntry.getSvg();
                if (svg == null) { // not existing yet? Render it!
                    svg = converter.conversion(MathoidEndpoints.SVG_ENDPOINT, mml, MathoidTypes.MML);
                    cacheEntry.setSvg(svg);
                }

                header.set("content-type", MathoidEndpoints.SVG_ENDPOINT.getResponseMediaType());
                return new HttpEntity<>(svg, header);
            case "png":
                byte[] png = cacheEntry.getPng();
                if (png == null) { // not existing yet? Render it!
                    png = converter.conversion(MathoidEndpoints.PNG_ENDPOINT, mml, MathoidTypes.MML)
                            .getBytes(Charsets.UTF_8);
                    cacheEntry.setPng(png);
                }

                header.set("content-type", MathoidEndpoints.PNG_ENDPOINT.getResponseMediaType());
                return new HttpEntity<>(png, header);
            case "mml":
                header.set("content-type", MathoidEndpoints.MML_ENDPOINT.getResponseMediaType());
                return new HttpEntity<>(mml, header);
            default:
                return new ResponseEntity<>("Unknown Format", HttpStatus.BAD_REQUEST);
        }
    }
}
