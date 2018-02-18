package org.citeplag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulasearchengine.mathmlconverters.cas.SaveTranslatorWrapper;
import com.formulasearchengine.mathmlconverters.cas.TranslationResponse;
import com.formulasearchengine.mathmlconverters.latexml.LaTeXMLConverter;
import com.formulasearchengine.mathmlconverters.latexml.LaTeXMLServiceResponse;
import com.formulasearchengine.mathmlconverters.mathoid.EnrichedMathMLTransformer;
import com.formulasearchengine.mathmlconverters.mathoid.MathoidConverter;
import com.formulasearchengine.mathmlsim.similarity.MathPlag;
import com.formulasearchengine.mathmlsim.similarity.result.Match;
import com.formulasearchengine.mathmltools.mml.elements.MathDoc;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.citeplag.config.CASTranslatorConfig;
import org.citeplag.config.LateXMLConfig;
import org.citeplag.config.MathoidConfig;
import org.citeplag.util.Example;
import org.citeplag.util.ExampleLoader;
import org.citeplag.util.SimilarityResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for our little MathML Pipeline.
 * Here we have in total:
 * <p>
 * 1. two POST methods for our latex to mathml conversion via latexml and mathoid
 * 2. one POST method for the similarity comparison
 * 3. one GET method to load a predefined example
 *
 * @author Vincent Stange
 */
@RestController
@RequestMapping("/math")
public class MathController {

    private static Logger logger = Logger.getLogger(MathController.class);

    @Autowired
    private LateXMLConfig lateXMLConfig;

    @Autowired
    private MathoidConfig mathoidConfig;


    @Autowired
    private CASTranslatorConfig translatorConfig;

    /**
     * POST method for calling the LaTeXML service / installation.
     *
     * @param config  optional configuration, if null, system default will be used
     * @param rawLatex the original (generic) input tex format, needed if the latex parameter is semantic
     * @param latex   latex to be converted
     * @param request http request for logging
     * @return service response
     * @throws Exception anything that could go wrong
     */
    @PostMapping
    @ApiOperation(value = "Converts a Latex String via LaTeXML to MathML semantics.")
    public LaTeXMLServiceResponse convertLatexml(
            @RequestParam(required = false) String config,
            @RequestParam(required = false) String rawLatex,
            @RequestParam String latex,
            HttpServletRequest request) throws Exception {

        // if request configuration is given, use it.
        LateXMLConfig usedConfig = config != null
                ? new ObjectMapper().readValue(config, LateXMLConfig.class)
                : lateXMLConfig;

        LaTeXMLConverter laTeXMLConverter = new LaTeXMLConverter(usedConfig);

        // no url = use the local installation of latexml, otherwise use: url = online service
        LaTeXMLServiceResponse response;
        if (StringUtils.isEmpty(usedConfig.getUrl())) {
            logger.info("local latex conversion from: " + request.getRemoteAddr());
            response = laTeXMLConverter.runLatexmlc(latex);
        } else {
            logger.info("service latex conversion from: " + request.getRemoteAddr());
            response = laTeXMLConverter.convertLatexmlService(latex);
        }

        return postProcessingOnMML(rawLatex, response);
    }

    private LaTeXMLServiceResponse postProcessingOnMML(String originalInputTex, LaTeXMLServiceResponse response) {
        try {
            final MathDoc math = new MathDoc(MathDoc.tryFixHeader(response.getResult()));
            math.fixGoldCd();
            if (originalInputTex != null) {
                math.changeTeXAnnotation(originalInputTex);
            }
            String newMML = math.toString();
            response.setResult(newMML);
        } catch (NullPointerException | ParserConfigurationException | IOException | SAXException e) {
            // write stack trace to string
            StringWriter sw = new StringWriter();
            PrintWriter printWriter = new PrintWriter(sw);
            e.printStackTrace(printWriter);
            String stackTrace = sw.toString();
            String oldLog = response.getLog();
            response.setLog(oldLog + System.lineSeparator()
                    + "Cannot post process MML response from Latexml. Reason: "
                    + e.getMessage()
                    + System.lineSeparator()
                    + stackTrace
            );
        }
        return response;
    }

    @PostMapping("/translation")
    @ApiOperation(value = "Translates a semantic LaTeX string to a given CAS.")
    public TranslationResponse translation(
            @RequestParam()
            @ApiParam(allowableValues = "Maple, Mathematica", required = true)
                    String cas,
            @RequestParam() String latex,
            HttpServletRequest request
    ) {
        logger.info("translation process to " + cas + " from: " + request.getRemoteAddr());
        SaveTranslatorWrapper translator = new SaveTranslatorWrapper();
        try {
            translator.init(
                    translatorConfig.getJarPath(),
                    cas,
                    translatorConfig.getReferencesPath()
            );
            translator.translate(latex);
            return translator.getTranslationResult();
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "[ERROR] " + e.toString();
            TranslationResponse response = new TranslationResponse();
            response.setLog(errorMsg);
            return response;
        }
    }

    /**
     * POST method for calling the Mathoid service.
     *
     * @param mathoidUrl optional url configuration, if null, system default will be used
     * @param latex      latex to be converted
     * @param request    http request for logging
     * @return mathml as string
     * @throws Exception anything that could go wrong
     */
    @PostMapping("/mathoid")
    @ApiOperation(value = "Converts a Latex String via Mathoid to MathML semantics.")
    public String convertMathoid(
            @RequestParam(required = false) String mathoidUrl,
            @RequestParam() String latex,
            HttpServletRequest request) throws Exception {

        // If local configuration is given, use it.
        mathoidUrl = mathoidUrl != null ? mathoidUrl : mathoidConfig.getUrl();

        MathoidConverter mathoidConverter = new MathoidConverter(new MathoidConfig().setUrl(mathoidUrl));
        try {
            logger.info("latex conversion via mathoid from: " + request.getRemoteAddr());
            String eMathML = mathoidConverter.convertLatex(latex);
            // transform enriched MathML to well-formed MathML (pMML + cMML)
            return new EnrichedMathMLTransformer(eMathML).getFullMathML();
        } catch (ResourceAccessException e) {
            return "mathoid not available under: " + mathoidUrl;
        } catch (Exception e) {
            logger.error("mathoid service error", e);
            return e.getMessage();
        }
    }

    @PostMapping(path = "similarity")
    @ApiOperation(value = "Get a list of similarities between two MathML semantics.")
    public SimilarityResult getSimilarities(
            @RequestParam(value = "mathml1") String mathmlA,
            @RequestParam(value = "mathml2") String mathmlB,
            @RequestParam(value = "type") String type,
            HttpServletRequest request) {

        try {
            List<Match> similarities;
            if (type.equals("similar")) {
                logger.info("similarity comparison from: " + request.getRemoteAddr());
                similarities = MathPlag.compareSimilarMathML(mathmlA, mathmlB);
            } else {
                logger.info("identical comparison from: " + request.getRemoteAddr());
                similarities = MathPlag.compareIdenticalMathML(mathmlA, mathmlB);
            }

            // also compare the original similarity factors
            Map<String, Object> originals = MathPlag.compareOriginalFactors(mathmlA, mathmlB);

            return new SimilarityResult("Okay", "", similarities, originals);
        } catch (Exception e) {
            logger.error("similarity error", e);
            return new SimilarityResult("Error", e.getMessage(), Collections.emptyList(), Maps.newTreeMap());
        }
    }

    /**
     * GET method to load an example and print the object out as a JSON.
     * (JSON transformation is done by spring)
     *
     * @return current example
     * @throws IOException requested example does not exist
     */
    @GetMapping(path = "example")
    @ApiOperation(value = "Get a full example for the demo.")
    public Example getExample() throws IOException {
        // this could easily be extended for more examples
        return new ExampleLoader().load("euler");
    }
}
