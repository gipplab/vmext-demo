package org.citeplag.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formulasearchengine.mathmltools.converters.LaTeXMLConverter;
import com.formulasearchengine.mathmltools.converters.services.LaTeXMLServiceResponse;
import com.formulasearchengine.mathmltools.mml.elements.MathDoc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.config.LaTeXMLRemoteConfig;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Andre Greiner-Petter
 */
public class LaTeXMLInterface {
    private static final Logger LOG = LogManager.getLogger(LaTeXMLInterface.class.getName());

    private LaTeXMLInterface() {
    }

    public static LaTeXMLServiceResponse convertLaTeX(
            LaTeXMLRemoteConfig defaultConfig,
            String config,
            String rawLatex,
            String latex,
            HttpServletRequest request
    ) throws JsonProcessingException {
        // if request configuration is given, use it.
        LaTeXMLRemoteConfig usedConfig = config != null
                ? new ObjectMapper().readValue(config, LaTeXMLRemoteConfig.class)
                : defaultConfig;

        LaTeXMLConverter laTeXMLConverter = new LaTeXMLConverter(usedConfig);

        if (usedConfig.isContent()) {
            laTeXMLConverter.semanticMode();
            Path p = Paths.get(usedConfig.getContentPath());
            laTeXMLConverter.redirectLatex(p);
        } else {
            laTeXMLConverter.nonSemanticMode();
        }

        LaTeXMLServiceResponse response;
        long time = System.currentTimeMillis();
        if (!usedConfig.isRemote()) {
            LOG.info("Call LaTeXML locally requested from: " + request.getRemoteAddr());
            response = new LaTeXMLServiceResponse(laTeXMLConverter.parseToNativeResponse(latex));
        } else {
            LOG.info("Call remote LaTeXML service from: " + request.getRemoteAddr());
            response = laTeXMLConverter.parseAsService(latex);
        }
        time = System.currentTimeMillis() - time;
        response.setLog(response.getLog() + " Time in MS: " + time);

        return LaTeXMLInterface.postProcessingOnMML(rawLatex, response);
    }

    private static LaTeXMLServiceResponse postProcessingOnMML(String originalInputTex, LaTeXMLServiceResponse response) {
        try {
            final MathDoc math = new MathDoc(MathDoc.tryFixHeader(response.getResult()));
            math.fixGoldCd();
            if (originalInputTex != null) {
                math.changeTeXAnnotation(originalInputTex);
            }
            String newMML = math.toString();
            response.setResult(newMML);
        } catch (NullPointerException | ParserConfigurationException | IOException | SAXException e) {
            // write stack trace to string
            StringWriter sw = new StringWriter();
            PrintWriter printWriter = new PrintWriter(sw);
            e.printStackTrace(printWriter);
            String stackTrace = sw.toString();
            String oldLog = response.getLog();
            response.setLog(oldLog + System.lineSeparator()
                    + "Cannot post process MML response from Latexml. Reason: "
                    + e.getMessage()
                    + System.lineSeparator()
                    + stackTrace
            );
        }
        return response;
    }
}
