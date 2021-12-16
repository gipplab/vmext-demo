package org.citeplag.beans;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.nist.drmf.interpreter.generic.mlp.pojo.SemanticEnhancedDocument;
import org.citeplag.converter.SemanticEnhancedDocumentDeserializer;

/**
 * @author Andre Greiner-Petter
 */
@JsonDeserialize(using = SemanticEnhancedDocumentDeserializer.class)
public class SemanticEnhancedDocumentExtension extends SemanticEnhancedDocument {
    public SemanticEnhancedDocumentExtension() {
        super();
    }

    public SemanticEnhancedDocumentExtension(SemanticEnhancedDocument sed) {
        // use the copy constructor
        super(sed);
    }
}
