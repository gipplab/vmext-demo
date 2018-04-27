package org.citeplag.util;

import com.formulasearchengine.mathmltools.converters.mathoid.MathoidInfoResponse;
import org.w3c.dom.Document;

/**
 * @author Andre Greiner-Petter
 */
public class MMLEndpointCache {

    private MathoidRequest originalRequest;
    private MathoidInfoResponse correspondingResponse;

    // The mml as string
    private Document mml;

    // The SVG image as string
    private String svg;

    // The png in raw data
    private byte[] png;

    public MMLEndpointCache(MathoidRequest originalRequest, MathoidInfoResponse response) {
        this.originalRequest = originalRequest;
        this.correspondingResponse = response;
    }

    public MathoidRequest getOriginalRequest() {
        return originalRequest;
    }

    public MathoidInfoResponse getCorrespondingResponse() {
        return correspondingResponse;
    }

    public Document getMml() {
        return mml;
    }

    public void setMml(Document mml) {
        this.mml = mml;
    }

    public String getSvg() {
        return svg;
    }

    public void setSvg(String svg) {
        this.svg = svg;
    }

    public byte[] getPng() {
        return png;
    }

    public void setPng(byte[] png) {
        this.png = png;
    }
}
