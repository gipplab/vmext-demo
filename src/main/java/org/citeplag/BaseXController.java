package org.citeplag;

import com.formulasearchengine.mathosphere.basex.Client;
import com.formulasearchengine.mathosphere.basex.Server;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.citeplag.config.BaseXConfig;
import org.citeplag.domain.MathRequest;
import org.citeplag.domain.MathUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
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
    public MathRequest mwsProcessing(
            @RequestParam String query,
            HttpServletRequest request) {
        return process(query, "mws", request);
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
                    value = "List of integers. Separate integers by ','!",
                    required = true) String integerArray,
            @RequestParam("MML") String harvest,
            HttpServletRequest request) {
        if (!startServerIfNecessary()) {
            LOG.warn("Return null for request, because BaseX server is not running.");
            return null;
        }
        LOG.info("Request updating given math from: " + request.getRemoteAddr());

        Integer[] delete;

        try {
            String cleaned = integerArray.replaceAll(" ", "");
            String[] elements = cleaned.split(",");
            delete = new Integer[elements.length];
            for (int i = 0; i < elements.length; i++) {
                delete[i] = Integer.parseInt(elements[i]);
            }
            LOG.info("Parsed integer arguments: " + Arrays.toString(delete));
        } catch (Exception e) {
            LOG.error("Cannot parse list of integers!", e);
            return null;
        }

        MathUpdate mu = new MathUpdate(delete, harvest);
        return mu.run();
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
