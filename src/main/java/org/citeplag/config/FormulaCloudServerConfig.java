package org.citeplag.config;

import com.formulasearchengine.formulacloud.es.ElasticsearchConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Andre Greiner-Petter
 */
@Component
@ConfigurationProperties(prefix = "formulacloud")
public class FormulaCloudServerConfig extends ElasticsearchConfig {
}
