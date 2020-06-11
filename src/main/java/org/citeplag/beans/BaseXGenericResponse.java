package org.citeplag.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Andre Greiner-Petter
 */
public class BaseXGenericResponse {

    @JsonProperty("status")
    private Integer code;

    @JsonProperty("message")
    private String message;

    public BaseXGenericResponse() {
    }

    public BaseXGenericResponse(int code, String message) {
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
