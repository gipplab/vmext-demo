package org.citeplag.config;

import gov.nist.drmf.interpreter.common.config.GenericLacastConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Andre Greiner-Petter
 */
@Component
@ConfigurationProperties(prefix = "generic-translator")
public class GenericLatexTranslatorConfig extends GenericLacastConfig {
    public GenericLatexTranslatorConfig() {
        super();
    }

    public GenericLatexTranslatorConfig(GenericLacastConfig reference) {
        super(reference);
        this.setMapleSubprocessInfo(new SpringMapleSubprocessInfo());
    }

    public static GenericLatexTranslatorConfig getDefaultConfig() {
        GenericLacastConfig defaultConfig = GenericLacastConfig.getDefaultConfig();
        return new GenericLatexTranslatorConfig(defaultConfig);
    }
}
