package org.citeplag.beans;


import com.formulasearchengine.formulacloud.beans.SearchResults;
import com.formulasearchengine.formulacloud.data.MOIResult;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SearchResultResponse extends SearchResults {
    public SearchResultResponse() {
        super("", null);
    }

    public SearchResultResponse(SearchResults results) {
        super(results.getSearchQuery(), results.getResults());
    }

    public SearchResultResponse(String query, List<MOIResult> results) {
        super(query, results);
    }

    @ResponseBody
    public String stringRepresentation() {
        return toString();
    }
}
