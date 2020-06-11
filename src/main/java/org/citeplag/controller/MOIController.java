package org.citeplag.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.formulasearchengine.formulacloud.FormulaCloudSearcher;
import com.formulasearchengine.formulacloud.beans.InverseDocumentFrequencies;
import com.formulasearchengine.formulacloud.beans.TermFrequencies;
import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.MathElement;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import com.formulasearchengine.mathmltools.converters.services.LaTeXMLServiceResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.SearchResultResponse;
import org.citeplag.config.FormulaCloudServerConfig;
import org.citeplag.config.LaTeXMLRemoteConfig;
import org.citeplag.endpoints.IMOISearchEndpoints;
import org.citeplag.util.DFCalculatorBinder;
import org.citeplag.util.LaTeXMLInterface;
import org.citeplag.util.MOIDatabaseBinder;
import org.citeplag.util.TFCalculatorBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Andre Greiner-Petter
 */
@RestController
@RequestMapping("/moi")
public class MOIController implements IMOISearchEndpoints {
    private static final Logger LOG = LogManager.getLogger(MOIController.class.getName());

    private LaTeXMLRemoteConfig laTeXMLRemoteConfig;

    private final FormulaCloudSearcher searcher;

    public MOIController() {
        this.searcher = new FormulaCloudSearcher(new FormulaCloudServerConfig());
    }

    @Autowired
    public void setServerConfig(FormulaCloudServerConfig serverConfig) {
        LOG.info("Suddenly the setter is called, interesting!");
        this.searcher.changeConnection(serverConfig);
    }

    @Autowired
    public void setLaTeXMLRemoteConfig(LaTeXMLRemoteConfig laTeXMLRemoteConfig) {
        this.laTeXMLRemoteConfig = laTeXMLRemoteConfig;
    }

    @PostConstruct
    public void init() {
        this.searcher.start();
    }

    @InitBinder("database")
    public void initDBBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(Databases.class, new MOIDatabaseBinder());
    }

    @InitBinder("tfCalculator")
    public void initTFCBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(TermFrequencies.class, new TFCalculatorBinder());
    }

    @InitBinder("idfCalculator")
    public void initDFCBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(InverseDocumentFrequencies.class, new DFCalculatorBinder());
    }

    @PostMapping("/getMOIStringByLaTeX")
    @ApiOperation(value = "Get the global frequency information of an MOI by it's LaTeX representation")
    public String getMOIStringByLaTeX(
            @ApiParam(value = "The string representation of an MOI", example = "\\sqrt{x}", required = true)
            @RequestParam() String moiLaTeX,
            @RequestParam(required = false) String config,
            HttpServletRequest request
    ) throws JsonProcessingException {
        LaTeXMLServiceResponse response = LaTeXMLInterface.convertLaTeX(laTeXMLRemoteConfig, config, null, moiLaTeX, request);
        String mml = response.getResult();
        return MOIConverter.mmlToString(mml);
    }

    @PostMapping("/getMOIByLaTeX")
    @ApiOperation(value = "Get the global frequency information of an MOI by it's LaTeX representation")
    public MathElement getMOIByLaTeX(
            @ApiParam(value = "The string representation of an MOI", example = "\\sqrt{x}", required = true)
            @RequestParam() String moiLaTeX,
            @ApiParam(value = "The database to get the MOI from", required = true)
            @RequestParam(defaultValue = "ARQMath") Databases database,
            @RequestParam(required = false) String config,
            HttpServletRequest request
    ) throws JsonProcessingException {
        String mml = getMOIStringByLaTeX(moiLaTeX, config, request);
        return searcher.getMOI(mml, database);
    }

    @GetMapping("/getMOIByString")
    @ApiOperation(value = "Get the global frequency information of an MOI by it's string representation")
    public MathElement getMOIByString(
            @ApiParam(value = "The string representation of an MOI", example = "mi:z", required = true)
            @RequestParam() String moi,
            @ApiParam(value = "The database to get the MOI from", required = true)
            @RequestParam(defaultValue = "ARQMath") Databases database
    ) {
        return searcher.getMOI(moi, database);
    }

    @GetMapping("/getMOIByMD5")
    @ApiOperation(value = "Get the global frequency information of an MOI by it's MD5 (base64) representation")
    public MathElement getMOIByMD5(
            @ApiParam(value = "The MD5 (base64) representation of an MOI", example = "okbFpXNoPCdHP06riJ9Ulg==", required = true)
            @RequestParam() String moi,
            @ApiParam(value = "The database to get the MOI from", required = true)
            @RequestParam(defaultValue = "ARQMath") Databases database
    ) {
        return searcher.getMOIByMD5(moi, database);
    }

    @GetMapping("/getMD5OfMOIString")
    @ApiOperation(
            value = "Converts the string representation of an MOI to the MD5 representation.",
            consumes = "text/plain",
            produces = "text/plain"
    )
    public String getMOIMd5(
            @ApiParam(value = "The string representation of an MOI", example = "mi:z", required = true)
            @RequestParam() String moi
    ) {
        return MOIConverter.getMD5(moi);
    }

    @PostMapping("/convertMOIStringToMML")
    @ApiOperation(
            value = "Converts the string representation of an MOI to the MathML representation.",
            consumes = "text/plain",
            produces = "application/xml"
    )
    public String convertMOIStringToMML(
            @ApiParam(value = "The string representation of an MOI", example = "mi:z", required = true)
            @RequestParam() String moi
    ) {
        return MOIConverter.stringToMML(moi);
    }

    @PostMapping("/convertMOIMMLToString")
    @ApiOperation(
            value = "Converts the MathML representation of an MOI to the string representation.",
            consumes = "application/xml",
            produces = "text/plain"
    )
    public String convertMOIMMLToString(
            @ApiParam(value = "The string representation of an MOI", example = "<math><mi>z</mi></math>", required = true)
            @RequestParam() String mml
    ) {
        return MOIConverter.mmlToString(mml);
    }

    @Override
    public SearchResultResponse search(
            SearchConfig config
    ) {
        return new SearchResultResponse(searcher.search(config));
    }
}
