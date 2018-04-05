package org.citeplag.util;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class Response {

    @JsonProperty("status")
    private Integer code;

    @JsonProperty("message")
    private String message;

    public Response() {
    }

    public Response(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
