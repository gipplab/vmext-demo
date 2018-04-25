package org.citeplag.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.formulasearchengine.mathmltools.converters.mathoid.MathoidTypes;
import com.google.common.base.Charsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * @author Andre Greiner-Petter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MathoidRequest {
    private static final Logger LOG = LogManager.getLogger(MathoidRequest.class.getName());

    private static final String jsonFormat = "{\"q\":\"%s\",\"type\":\"%s\"}";

    @JsonProperty("q")
    private String q;

    @JsonProperty("type")
    private String type;

    public MathoidRequest() {
    }

    public MathoidRequest(String q, String type) {
        this.q = q;
        this.type = type;
    }

    @JsonIgnore
    public String getValue() {
        return q;
    }

    public String getType() {
        return type;
    }

    /**
     * Base on https://github.com/wikimedia/mathoid/blob/master/lib/math.js#L204-L223
     * @return mathoid type
     */
    @JsonIgnore
    public MathoidTypes getCorrespondingType() {
        switch (type) {
            case "tex":
                return MathoidTypes.TEX;
            case "inline-tex":
                return MathoidTypes.INLINE_TEX;
            case "ascii":
            case "asciimathml":
            case "asciimath":
                return MathoidTypes.ASCII;
            case "mml":
            case "pmml":
            case "mathml":
                return MathoidTypes.MML;
            case "chem":
                return MathoidTypes.CHEM;
            default: return null;
        }
    }

    @Override
    public String toString() {
        return String.format(jsonFormat, q, type);
    }

    public String sha1Hash() {
        return DigestUtils.sha1Hex(toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MathoidRequest)) {
            return false;
        }
        MathoidRequest mreq = (MathoidRequest) obj;
        return this.q.equals(mreq.q) && this.type.equals(mreq.type);
    }

    public static void main(String[] args){
        MathoidRequest r = new MathoidRequest("c^2 = a^2 + b^2", "tex");
        r.sha1Hash();
        System.out.println(DigestUtils.sha1Hex("{\"q\":\"c^2 = a^2 + b^2\",\"type\":\"tex\"}"));
        System.out.println(DigestUtils.sha1Hex("{\"q\":\"qwertz\",\"type\":\"tex\"}"));
        String s = "{\"success\":true,\"checked\":\"a^{2}=b^{2}+c^{2}\",\"requiredPackages\":[],\"identifiers\":[\"a\",\"b\",\"c\"],\"endsWithDot\":false}";
        System.out.println(DigestUtils.sha1Hex(s));
        System.out.println(DigestUtils.sha1Hex("{\"q\":\"a^{2}=b^{2}+c^{2}\",\"type\":\"tex\"}"));
    }
}
