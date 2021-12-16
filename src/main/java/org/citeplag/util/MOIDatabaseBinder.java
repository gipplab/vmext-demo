package org.citeplag.util;

import com.formulasearchengine.formulacloud.data.Databases;

import java.beans.PropertyEditorSupport;

/**
 * @author Andre Greiner-Petter
 */
public class MOIDatabaseBinder extends PropertyEditorSupport {
    @Override
    public void setAsText(String db) {
        setValue(Databases.getByString(db));
    }
}
