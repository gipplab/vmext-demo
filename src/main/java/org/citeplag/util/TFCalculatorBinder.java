package org.citeplag.util;

import com.formulasearchengine.formulacloud.beans.TermFrequencies;

import java.beans.PropertyEditorSupport;

/**
 * @author Andre Greiner-Petter
 */
public class TFCalculatorBinder extends PropertyEditorSupport {
    @Override
    public void setAsText(String tf) {
        setValue(TermFrequencies.getTermFrequencyByTag(tf));
    }
}
