package org.citeplag.endpoints;

import com.formulasearchengine.formulacloud.data.Databases;
import com.formulasearchengine.formulacloud.data.SearchConfig;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.citeplag.util.SearchResultResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * @author Andre Greiner-Petter
 */
public interface IMOISearchEndpoints {
    @PostMapping("/search")
    @ApiOperation(value = "Searches for MOIs by a given text.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully searched", response = SearchResultResponse.class),
                    @ApiResponse(code = 500, message = "Unable to connect with database" )
            }
    )
    default SearchResultResponse searchAsJson(
            @ApiParam(value = "The text search query", required = true)
            @RequestParam() String query,
            @ApiParam(value = "The database you want search", defaultValue = "ARQMath")
            @RequestParam(required = false) Databases database,
            @ApiParam(value = "The minimum term frequency of the MOI", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer minTF,
            @ApiParam(value = "The minimum document frequency of the MOI", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer minDF,
            @ApiParam(value = "The minimum complexity of the MOI", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer minC,
            @ApiParam(value = "The maximum term frequency of the MOI", example = Integer.MAX_VALUE+"")
            @RequestParam(required = false, defaultValue = Integer.MAX_VALUE+"") Integer maxTF,
            @ApiParam(value = "The maximum document frequency of the MOI", example = Integer.MAX_VALUE+"")
            @RequestParam(required = false, defaultValue = Integer.MAX_VALUE+"") Integer maxDF,
            @ApiParam(value = "The maximum complexity of the MOI", example = Integer.MAX_VALUE+"")
            @RequestParam(required = false, defaultValue = Integer.MAX_VALUE+"") Integer maxC,
            @ApiParam(value = "The number of documents to retrieve from the database", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer numberOfDocsToRetrieve,
            @ApiParam(value = "The minimum number of retrieved documents an MOI should appear in. " +
                    "For example, you can only consider MOI that appear in at least 5 of the 10 retrieved documents.",
                    example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer minNumberOfDocHitsPerMOI,
            @ApiParam(value = "The maximum number of result MOI that should be returned", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer maxNumberOfResults,
            @ApiParam(value = "Weather the result MOI should be also returned in MathML representation or not. This " +
                    "could significantly increase the size of the response. Hence, the default value is false.",
                    example = "false")
            @RequestParam(required = false, defaultValue = "false") Boolean enableMathML,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        SearchConfig config = new SearchConfig(query);
        if ( database != null ) config.setDb(database);
        if ( minTF != null ) config.setMinGlobalTF(minTF);
        if ( minDF != null ) config.setMaxGlobalDF(minDF);
        if ( minC != null ) config.setMinComplexity(minC);
        if ( maxTF != null ) config.setMaxGlobalTF(maxTF);
        if ( maxDF != null ) config.setMaxGlobalDF(maxDF);
        if ( maxC != null ) config.setMaxComplexity(maxC);
        if ( numberOfDocsToRetrieve != null ) config.setNumberOfDocsToRetrieve(numberOfDocsToRetrieve);
        if ( minNumberOfDocHitsPerMOI != null ) config.setMinNumberOfDocHitsPerMOI(minNumberOfDocHitsPerMOI);
        if ( maxNumberOfResults != null ) config.setMaxNumberOfResults(maxNumberOfResults);
        if ( enableMathML != null ) config.setEnableMathML(enableMathML);
        return search(config);
    }

    SearchResultResponse search(SearchConfig config);
}
