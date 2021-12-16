package org.citeplag.util;

import com.formulasearchengine.formulacloud.beans.MathMergeFunctions;

import java.beans.PropertyEditorSupport;

/**
 * @author Andre Greiner-Petter
 */
public class MergeFunctionBinder extends PropertyEditorSupport {
    @Override
    public void setAsText(String func) {
        setValue(MathMergeFunctions.getFunctionByName(func));
    }
}
