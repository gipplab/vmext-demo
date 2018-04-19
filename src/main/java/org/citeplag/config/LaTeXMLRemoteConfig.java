package org.citeplag.config;

import com.formulasearchengine.mathmltools.converters.config.LaTeXMLConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Andre Greiner-Petter
 */
@Component
@ConfigurationProperties(prefix = "latexml")
public class LaTeXMLRemoteConfig extends LaTeXMLConfig {
    private boolean remote = false;

    private boolean content = false;

    private String contentPath = "";

    public LaTeXMLRemoteConfig() {
        this(LaTeXMLConfig.getDefaultConfiguration());
    }

    public LaTeXMLRemoteConfig(LaTeXMLConfig config) {
        setUrl(config.getUrl());
        setDefaultParams(config.getDefaultParams());
        setContentPreloads(config.getContentPreloads());
        setDefaultPreloads(config.getDefaultPreloads());
        setExtraContentParams(config.getExtraContentParams());
    }

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public boolean isContent() {
        return content;
    }

    public void setContent(boolean content) {
        this.content = content;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }
}
