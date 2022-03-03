package org.citeplag.controller;

import com.formulasearchengine.mathosphere.basex.Client;
import com.formulasearchengine.mathosphere.basex.Server;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.citeplag.config.BaseXConfig;
import org.citeplag.domain.MathRequest;
import org.citeplag.domain.MathUpdate;
import org.citeplag.beans.BaseXGenericResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Imported from a RestX frontend. Originally from mathosphere/restd.
 *
 * @author Andre Greiner-Petter
 */
@RestController
@RequestMapping("/basex")
public class BaseXController {
    private static final Logger LOG = LogManager.getLogger(BaseXController.class.getName());

    // the final BaseX server
    private static final Server BASEX_SERVER = Server.getInstance();
    private static boolean serverRunning = false;

    @Autowired
    private BaseXConfig baseXConfig;

    @PostMapping
    @ApiOperation(value = "Run query on BaseX")
    public MathRequest processing(
            @RequestParam @ApiParam(allowableValues = "tex, xquery, mws") String type,
            @RequestParam String query,
            HttpServletRequest request) {
        return process(query, type, request);
    }

    @PostMapping("/texquery")
    @ApiOperation(value = "Run TeX query on BaseX")
    public MathRequest texProcessing(
            @RequestParam String query,
            HttpServletRequest request) {
        return process(query, "tex", request);
    }

    @PostMapping("/xquery")
    @ApiOperation(value = "Run XQuery on BaseX")
    public MathRequest xQueryProcessing(
            @RequestParam String query,
            HttpServletRequest request) {
        return process(query, "xquery", request);
    }

    @PostMapping("/mwsquery")
    @ApiOperation(value = "Run MWS query on BaseX")
    public MathRequest mwsProcessing(@RequestBody String data, HttpServletRequest request) {
        String query = extractQueryFromData(data);
        if(query==null){ return null; }
        return process(query, "mws", request);
    }

    /**
     * Extracting the query input from url encoded string of data.
     * This is required to handle requests coming from FormulaSearch-extension.
     * @param data url encoded string which container query as json.
     * @return query as string or data
     */
    private String extractQueryFromData(String data){
        String query = null;
        try {
            String result = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8.name());
            JSONObject jsonObject = new JSONObject(result);
            query = jsonObject.get("query").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return query;
    }

    private MathRequest process(String query, String type, HttpServletRequest request) {
        if (!startServerIfNecessary()) {
            LOG.warn("Return null for request, because BaseX server is not running.");
            return null;
        }
        LOG.info("BaseX processing request from: " + request.getRemoteAddr());
        MathRequest mreq = new MathRequest(query);
        mreq.setType(type);
        return mreq.run();
    }

    @PostMapping("/update")
    @ApiOperation(value = "Update results via BaseX")
    public MathUpdate update(
            @RequestParam("Deletions") @ApiParam(
                    name = "Deletions",
                    value = "List of integers. Separate integers by ','!")
                    String integerArray,
            @RequestParam(name = "MML", required = false) String harvest,
            HttpServletRequest request) {
        if (!startServerIfNecessary()) {
            LOG.warn("Return null for request, because BaseX server is not running.");
            return null;
        }
        LOG.info("Request updating given math from: " + request.getRemoteAddr());

        Integer[] delete;

        try {
            delete = parseArray(integerArray);
        } catch (NumberFormatException e) {
            LOG.error("Cannot parse list of integers!", e);
            return null;
        }

        // replace null by empty string to avoid null pointers
        String secureHarvest = harvest == null ? "" : harvest;

        MathUpdate mu = new MathUpdate(delete, secureHarvest);
        return mu.run();
    }

    /**
     * Try to parse an integer array given as a string.
     * If the given string is null or empty, or the string cannot be parsed
     * it will return an empty array.
     *
     * @param integerArray
     * @return
     */
    private Integer[] parseArray(String integerArray) throws NumberFormatException {
        if (integerArray == null || integerArray.isEmpty()) {
            return new Integer[0];
        }

        String cleaned = integerArray.replaceAll(" ", "");
        String[] elements = cleaned.split(",");
        Integer[] delete = new Integer[elements.length];
        for (int i = 0; i < elements.length; i++) {
            delete[i] = Integer.parseInt(elements[i]);
        }

        LOG.info("Parsed integer arguments: " + Arrays.toString(delete));
        return delete;
    }

    @PostMapping("/countRev")
    @ApiOperation(value = "Count the number of formulae with specified revision number")
    public Integer dvsize(
            @RequestParam Integer revision,
            HttpServletRequest request) {
        if (!startServerIfNecessary()) {
            LOG.warn("Return null for request, because BaseX server is not running.");
            return null;
        }
        LOG.info("BaseX request to count number of formulae with revision number from: " + request.getRemoteAddr());
        Client client = new Client();
        return client.countRevisionFormula(revision);
    }

    @PostMapping("/countAll")
    @ApiOperation(value = "Count the total number of formulae")
    public Integer dvsize(HttpServletRequest request) {
        if (!startServerIfNecessary()) {
            LOG.warn("Return null for request, because BaseX server is not running.");
            return null;
        }
        LOG.info("BaseX request to count total number of formulae from: " + request.getRemoteAddr());
        Client client = new Client();
        return client.countAllFormula();
    }

    @PostMapping("/restartBaseXServer")
    @ApiOperation(value = "Restarts the BaseX server with another harvest file.")
    public BaseXGenericResponse restart(
            @RequestParam("Path") @ApiParam(
                    name = "Path",
                    value = "Path to harvest file (linux line separators)!",
                    required = true)
                    String harvestPath,
            HttpServletRequest request) {
        if (harvestPath == null || harvestPath.isEmpty()) {
            return new BaseXGenericResponse(1, "Empty path! Didn't restart the server.");
        }

        String oldHarvest = baseXConfig.getHarvestPath();

        try {
            Path path = Paths.get(harvestPath);

            if (!Files.exists(path)) {
                return new BaseXGenericResponse(1, "Given file does not exist! Didn't restart server.");
            }

            baseXConfig.setHarvestPath(harvestPath);

            BASEX_SERVER.startup(path.toFile());
            return new BaseXGenericResponse(0, "Restarted BaseX server with harvest file " + path.toString());
        } catch (InvalidPathException ipe) {
            return new BaseXGenericResponse(1, "Cannot parse given string to a path, "
                    + "didn't restart server! Reason: " + ipe.getReason());
        } catch (IOException ioe) {
            // reset settings
            serverRunning = false;
            baseXConfig.setHarvestPath(oldHarvest);
            return new BaseXGenericResponse(1, "Cannot restart the BaseX server. " + ioe.toString());
        }
    }

    private boolean startServerIfNecessary() {
        if (!serverRunning) {
            LOG.info("Startup basex server with harvest file: " + baseXConfig.getHarvestPath());
            Path path = Paths.get(baseXConfig.getHarvestPath());
            try {
                BASEX_SERVER.startup(path.toFile());
                serverRunning = true;
            } catch (IOException e) {
                LOG.error("Cannot load harvest file to start BaseX server.", e);
            }
        }
        return serverRunning;
    }
}
