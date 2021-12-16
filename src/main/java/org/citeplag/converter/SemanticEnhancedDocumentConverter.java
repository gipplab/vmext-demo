package org.citeplag.converter;

import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.SemanticEnhancedDocumentExtension;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocumentConverter extends AbstractHttpMessageConverter<SemanticEnhancedDocument> {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancedDocumentConverter.class.getName());

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.isInstance(new SemanticEnhancedDocumentExtension());
    }

    @Override
    protected SemanticEnhancedDocument readInternal(Class<? extends SemanticEnhancedDocument> clazz, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        String arg = IOUtils.toString(httpInputMessage.getBody(), StandardCharsets.UTF_8.toString());
        LOG.warn("Read Internal, but if is the argument?: " + arg);
        return SemanticEnhancedDocument.deserialize(arg);
    }

    @Override
    protected void writeInternal(SemanticEnhancedDocument sed, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        LOG.info("Serializing SED");
        byte[] bytes = sed.serialize().getBytes();
        httpOutputMessage.getBody().write(bytes);
    }
}
