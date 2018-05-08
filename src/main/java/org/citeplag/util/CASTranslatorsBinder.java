package org.citeplag.util;

import java.beans.PropertyEditorSupport;

/**
 * Defines a binder for Spring MVC
 *
 * @author Andre Greiner-Petter
 */
public class CASTranslatorsBinder extends PropertyEditorSupport {
    @Override
    public void setAsText(String cas) {
        setValue(CASTranslators.getTranslatorType(cas));
    }
}
