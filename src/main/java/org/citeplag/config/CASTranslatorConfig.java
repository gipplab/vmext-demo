package org.citeplag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Andre Greiner-Petter
 */
@Component
@ConfigurationProperties(prefix = "translator")
public class CASTranslatorConfig extends com.formulasearchengine.mathmlconverters.cas.TranslatorConfig {
}
