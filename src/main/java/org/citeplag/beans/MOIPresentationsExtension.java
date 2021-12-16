package org.citeplag.beans;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.nist.drmf.interpreter.generic.mlp.pojo.MOIPresentations;
import org.citeplag.converter.MOIPresentationsDeserializer;

/**
 * @author Andre Greiner-Petter
 */
@JsonDeserialize(using = MOIPresentationsDeserializer.class)
public class MOIPresentationsExtension extends MOIPresentations {
    public MOIPresentationsExtension() {
        super();
    }

    public MOIPresentationsExtension(MOIPresentations moi) {
        super(moi);
    }
}
