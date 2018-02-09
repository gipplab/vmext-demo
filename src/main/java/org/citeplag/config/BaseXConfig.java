package org.citeplag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Andre Greiner-Petter
 */
@Component
@ConfigurationProperties(prefix = "basexserver")
public class BaseXConfig {

    private String harvestPath;

    public String getHarvestPath() {
        return harvestPath;
    }

    public void setHarvestPath(String harvestPath) {
        this.harvestPath = harvestPath;
    }
}
