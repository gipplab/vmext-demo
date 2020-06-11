package org.citeplag.util;

import com.formulasearchengine.formulacloud.beans.InverseDocumentFrequencies;

import java.beans.PropertyEditorSupport;

/**
 * @author Andre Greiner-Petter
 */
public class DFCalculatorBinder extends PropertyEditorSupport {
    @Override
    public void setAsText(String idf) {
        setValue(InverseDocumentFrequencies.getInverseDocumentFrequencyByTag(idf));
    }
}
