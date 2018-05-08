package org.citeplag.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.formulasearchengine.mathmltools.converters.cas.SaveTranslatorWrapper;
import org.citeplag.config.CASTranslatorConfig;

/**
 * @author Andre Greiner-Petter
 */
public enum CASTranslators {
    MapleTranslator("Maple", null),
    MathematicaTranslator("Mathematica", null);

    private String cas;

    @JsonIgnore
    private SaveTranslatorWrapper translator;

    CASTranslators(String cas, SaveTranslatorWrapper translator) {
        this.cas = cas;
        this.translator = translator;
    }

    public static void init(CASTranslatorConfig config) throws Exception {
        for (CASTranslators ct : CASTranslators.values()) {
            if (ct.translator != null) {
                continue;
            }

            ct.translator = new SaveTranslatorWrapper();
            ct.translator.init(
                    config.getJarPath(),
                    ct.cas,
                    config.getReferencesPath()
            );
        }
    }

    /**
     * May return null if the requested CAS was unknown.
     *
     * @param cas Maple or Mathematica
     * @return corresponding enum object for CAS translator
     */
    public static CASTranslators getTranslatorType(String cas) {
        for (CASTranslators ct : CASTranslators.values()) {
            if (cas.equals(ct.cas)) {
                return ct;
            }
        }

        return null;
    }

    @JsonIgnore
    public SaveTranslatorWrapper getTranslator() {
        return translator;
    }

    @Override
    public String toString() {
        return cas;
    }
}
