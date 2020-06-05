package org.citeplag;

import com.formulasearchengine.formulacloud.FormulaCloudSearcher;
import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.MathElement;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import com.formulasearchengine.formulacloud.util.MOIConverter;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.config.FormulaCloudServerConfig;
import org.citeplag.endpoints.IMOISearchEndpoints;
import org.citeplag.util.MOIDatabaseBinder;
import org.citeplag.util.SearchResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

/**
 * @author Andre Greiner-Petter
 */
@RestController
@RequestMapping("/moi")
public class MOIController implements IMOISearchEndpoints {
    private static final Logger LOG = LogManager.getLogger(MOIController.class.getName());

    private FormulaCloudSearcher searcher;

    public MOIController() {
        this.searcher = new FormulaCloudSearcher(new FormulaCloudServerConfig());
    }

    @Autowired
    public void setServerConfig(FormulaCloudServerConfig serverConfig) {
        LOG.info("Suddenly the setter is called, interesting!");
        this.searcher.changeConnection(serverConfig);
    }

    @PostConstruct
    public void init() {
        this.searcher.start();
    }

    @InitBinder("database")
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(Databases.class, new MOIDatabaseBinder());
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
