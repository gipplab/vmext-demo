package org.citeplag.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.SemanticEnhancedDocumentExtension;

import java.io.IOException;

/**
 * @author Andre Greiner-Petter
 */
public class SemanticEnhancedDocumentDeserializer extends StdDeserializer<SemanticEnhancedDocumentExtension> {
    private static final Logger LOG = LogManager.getLogger(SemanticEnhancedDocumentDeserializer.class.getName());

    public SemanticEnhancedDocumentDeserializer() {
        this(null);
    }

    public SemanticEnhancedDocumentDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public SemanticEnhancedDocumentExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        LOG.info("Retrieved SED parameter. Try to parse it.");
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        try {
            SemanticEnhancedDocument sed = mapper.readValue(jsonParser, SemanticEnhancedDocument.class);
            LOG.info("Successfully parsed");
            return new SemanticEnhancedDocumentExtension(sed);
        } catch (Exception e) {
            LOG.error("Unable to parse:", e);
            return null;
        }
    }
}
