package org.citeplag.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.MOIPresentationsExtension;
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
public class MoiPresentationsConverter extends AbstractHttpMessageConverter<MOIPresentations> {
    private static final Logger LOG = LogManager.getLogger(MoiPresentationsConverter.class.getName());

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.isInstance(new MOIPresentationsExtension());
    }

    @Override
    protected MOIPresentations readInternal(Class<? extends MOIPresentations> clazz, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        String arg = IOUtils.toString(httpInputMessage.getBody(), StandardCharsets.UTF_8.toString());
        LOG.warn("Read Internal, but if is the argument?: " + arg);
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        return mapper.readValue(arg, MOIPresentations.class);
    }

    @Override
    protected void writeInternal(MOIPresentations moi, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        LOG.info("Serializing MOI");
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        String moiString = mapper.writeValueAsString(moi);
        httpOutputMessage.getBody().write(moiString.getBytes());
    }
}
