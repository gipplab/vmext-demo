package org.citeplag.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.citeplag.beans.MOIPresentationsExtension;

/**
 * @author Andre Greiner-Petter
 */
public class MOIPresentationsDeserializer extends StdDeserializer<MOIPresentationsExtension> {
    private static final Logger LOG = LogManager.getLogger(MOIPresentationsDeserializer.class.getName());

    public MOIPresentationsDeserializer() {
        this(null);
    }

    public MOIPresentationsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public MOIPresentationsExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        LOG.info("Retrieved SED parameter. Try to parse it.");
        ObjectMapper mapper = SemanticEnhancedDocument.getMapper();
        try {
            MOIPresentations moi = mapper.readValue(jsonParser, MOIPresentations.class);
            LOG.info("Successfully parsed");
            return new MOIPresentationsExtension(moi);
        } catch (Exception e) {
            LOG.error("Unable to parse:", e);
            return null;
        }
    }
}
