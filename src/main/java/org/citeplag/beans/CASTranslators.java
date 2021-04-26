package org.citeplag.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gov.nist.drmf.interpreter.cas.translation.SemanticLatexTranslator;
import org.citeplag.config.CASTranslatorConfig;

import java.util.Arrays;

/**
 * @author Andre Greiner-Petter
 */
public enum CASTranslators {
    MapleTranslator("Maple", null),
    MathematicaTranslator("Mathematica", null),
    SympyTranslator("SymPy", null);

    private String cas;

    @JsonIgnore
    private SemanticLatexTranslator translator;

    CASTranslators(String cas, SemanticLatexTranslator translator) {
        this.cas = cas;
        this.translator = translator;
    }

    public static void init(CASTranslatorConfig config) throws Exception {
        for (CASTranslators ct : CASTranslators.values()) {
            if (ct.translator != null) {
                continue;
            }

//            System.out.println("Load CAS jar from " + config.getJarPath());
//            System.out.println("Exists: " + Files.exists(Paths.get(config.getJarPath())));
            ct.translator = new SemanticLatexTranslator(ct.cas);
        }
    }

    /**
     * May return null if the requested CAS was unknown.
     *
     * @param cas Maple or Mathematica
     * @return corresponding enum object for CAS translator
     */
    public static CASTranslators getTranslatorType(String cas) {
        return Arrays
                .stream(CASTranslators.values())
                .filter(e -> e.cas.equals(cas))
                .findFirst()
                .orElse(null);
    }

    @JsonIgnore
    public SemanticLatexTranslator getTranslator() {
        return translator;
    }

    @Override
    public String toString() {
        return cas;
    }
}
