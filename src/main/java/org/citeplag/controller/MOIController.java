package org.citeplag.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.formulasearchengine.formulacloud.FormulaCloudSearcher;
import com.formulasearchengine.formulacloud.beans.InverseDocumentFrequencies;
import com.formulasearchengine.formulacloud.beans.MathMergeFunctions;
import com.formulasearchengine.formulacloud.beans.TermFrequencies;
import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.MathElement;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import com.formulasearchengine.mathmltools.converters.services.LaTeXMLServiceResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.SearchResultResponse;
import org.citeplag.config.FormulaCloudServerConfig;
import org.citeplag.config.LaTeXMLRemoteConfig;
import org.citeplag.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author Andre Greiner-Petter
 */
@RestController
@RequestMapping("/moi")
@Validated
public class MOIController {
    private static final Logger LOG = LogManager.getLogger(MOIController.class.getName());

    private LaTeXMLRemoteConfig laTeXMLRemoteConfig;

    private final FormulaCloudSearcher searcher;

    public MOIController() {
        this.searcher = new FormulaCloudSearcher(new FormulaCloudServerConfig());
    }

    @Autowired
    public void setServerConfig(FormulaCloudServerConfig serverConfig) {
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

    @InitBinder("mergeFunction")
    public void initMergeFunctionBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(MathMergeFunctions.class, new MergeFunctionBinder());
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

    @PostMapping(
            value = "/search",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    @ApiOperation(value = "Searches for MOIs by a given text.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully searched", response = SearchResultResponse.class),
                    @ApiResponse(code = 500, message = "Unable to connect with database")
            }
    )
    public SearchResultResponse searchAsJson(
            @ApiParam(value = "The text search query", required = true)
            @RequestParam() String query,
            @ApiParam(value = "The database you want search", defaultValue = "ARQMath")
            @RequestParam(required = false) Databases database,
            @ApiParam(name = "tfCalculator", value = "Specifies how to calculate the term frequency.", defaultValue = "mBM25")
            @RequestParam(name = "tfCalculator", required = false) TermFrequencies tfCalculator,
            @ApiParam(value = "Specifies how to calculate the inverse document frequency.",
                    name = "idfCalculator", defaultValue = "IDF")
            @RequestParam(name = "idfCalculator", required = false) InverseDocumentFrequencies idfCalculator,
            @ApiParam(value = "Specifies how to merge two MOI results by their scores. Either pick the minimum, maximum or calculate the average score.",
                    name = "mergeFunction", defaultValue = "MAX")
            @RequestParam(name = "mergeFunction", required = false) MathMergeFunctions mergeFunction,
            @ApiParam(value = "k1 is used if you calculate the BM25 or mBM25 term frequency score. Otherwise this value is ignored.",
                    defaultValue = "1.2", example = "1.2")
            @RequestParam(required = false) Double k1,
            @ApiParam(value = "b is used if you calculate the BM25 or mBM25 term frequency score. Otherwise this value is ignored.",
                    defaultValue = "0.95", example = "0.95")
            @Min(value = 0, message = "The parameter b must be in [0, 1]")
            @Max(value = 1, message = "The parameter b must be in [0, 1]")
            @RequestParam(required = false) Double b,
            @ApiParam(value = "The minimum term frequency of the MOI", example = "1")
            @Min(value = 1, message = "The minimum term frequency is 1")
            @RequestParam(required = false, defaultValue = "1") Integer minTF,
            @ApiParam(value = "The minimum document frequency of the MOI", example = "1")
            @Min(value = 1, message = "The minimum document frequency is 1")
            @RequestParam(required = false, defaultValue = "1") Integer minDF,
            @ApiParam(value = "The minimum complexity of the MOI", example = "1")
            @Min(value = 1, message = "The minimum complexity is 1")
            @RequestParam(required = false, defaultValue = "1") Integer minC,
            @ApiParam(value = "The maximum term frequency of the MOI", example = Integer.MAX_VALUE + "")
            @Min(value = 1, message = "The minimum value is 1")
            @Max(value = Integer.MAX_VALUE, message = "The maximum supported value is a 32b signed integer")
            @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") Integer maxTF,
            @ApiParam(value = "The maximum document frequency of the MOI", example = Integer.MAX_VALUE + "")
            @Min(value = 1, message = "The minimum value is 1")
            @Max(value = Integer.MAX_VALUE, message = "The maximum supported value is a 32b signed integer")
            @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") Integer maxDF,
            @ApiParam(value = "The maximum complexity of the MOI", example = Integer.MAX_VALUE + "")
            @Min(value = 1, message = "The minimum value is 1")
            @Max(value = Integer.MAX_VALUE, message = "The maximum supported value is a 32b signed integer")
            @RequestParam(required = false, defaultValue = Integer.MAX_VALUE + "") Integer maxC,
            @ApiParam(value = "The number of documents to retrieve from the database", example = "10")
            @Min(value = 0, message = "You can not search for less than 0 documents")
            @Max(value = 500, message = "For performance reasons, we do not allow to search for more than 500 documents.")
            @RequestParam(required = false, defaultValue = "10") Integer numberOfDocsToRetrieve,
            @ApiParam(value = "The minimum number of retrieved documents an MOI should appear in. "
                    + "For example, you can only consider MOI that appear in at least 5 of the 10 retrieved documents.",
                    example = "1")
            @Min(value = 1, message = "The logical minimum is 1")
            @RequestParam(required = false, defaultValue = "1") Integer minNumberOfDocHitsPerMOI,
            @ApiParam(value = "The maximum number of result MOI that should be returned", example = "10")
            @Max(value = 1000, message = "For performance and traffic reason, you can only request the top 1000 hits.")
            @RequestParam(required = false, defaultValue = "10") Integer maxNumberOfResults,
            @ApiParam(value = "Weather the result MOI should be also returned in MathML representation or not. This "
                    + "could significantly increase the size of the response. Hence, the default value is false.",
                    example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean enableMathML
    ) {
        SearchConfig config = new SearchConfig(query);
        if (database != null) {
            config.setDb(database);
        }
        if (minTF != null) {
            config.setMinGlobalTF(minTF);
        }
        if (minDF != null) {
            config.setMaxGlobalDF(minDF);
        }
        if (minC != null) {
            config.setMinComplexity(minC);
        }
        if (maxTF != null) {
            config.setMaxGlobalTF(maxTF);
        }
        if (maxDF != null) {
            config.setMaxGlobalDF(maxDF);
        }
        if (maxC != null) {
            config.setMaxComplexity(maxC);
        }
        if (numberOfDocsToRetrieve != null) {
            config.setNumberOfDocsToRetrieve(numberOfDocsToRetrieve);
        }
        if (minNumberOfDocHitsPerMOI != null) {
            config.setMinNumberOfDocHitsPerMOI(minNumberOfDocHitsPerMOI);
        }
        if (maxNumberOfResults != null) {
            config.setMaxNumberOfResults(maxNumberOfResults);
        }
        if (enableMathML != null) {
            config.setEnableMathML(enableMathML);
        }
        if (tfCalculator != null) {
            config.getTfidfOptions().setTfOption(tfCalculator);
        }
        if (idfCalculator != null) {
            config.getTfidfOptions().setIdfOption(idfCalculator);
        }
        if (mergeFunction != null) {
            config.setScoreMerger(mergeFunction);
        }
        if (k1 != null) {
            config.getTfidfOptions().setK1(k1);
        }
        if (b != null) {
            config.getTfidfOptions().setB(b);
        }
        return search(config);
    }

    private SearchResultResponse search(SearchConfig config) {
        return new SearchResultResponse(searcher.search(config));
    }
}
