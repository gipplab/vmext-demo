package org.citeplag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Andre Greiner-Petter
 */
@Component
@ConfigurationProperties(prefix = "formulacloud")
public class FormulaCloudConfig {
    private String tfidfData;

    private String databasePath;

    private String index;

    private int esMaxHits;

    private int minDF;

    private int maxDF;

    public FormulaCloudConfig() {}

    public void setTfidfData(String tfidfData) {
        this.tfidfData = tfidfData;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setEsMaxHits(int esMaxHits) {
        this.esMaxHits = esMaxHits;
    }

    public void setMinDF(int minDF) {
        this.minDF = minDF;
    }

    public void setMaxDF(int maxDF) {
        this.maxDF = maxDF;
    }

//    public SearcherConfig getConfig() {
//        SearcherConfig conf = new SearcherConfig();
//        conf.setDatabaseParentFolder(databasePath);
//        conf.setTfidfData(tfidfData);
//        conf.setFixedIndex(index);
//        conf.setElasticsearchMaxHits(esMaxHits);
//        conf.setMinDocumentFrequency(minDF);
//        conf.setMaxDocumentFrequency(maxDF);
//        return conf;
//    }
}
