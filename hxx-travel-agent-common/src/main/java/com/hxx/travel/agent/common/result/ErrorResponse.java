package com.hxx.travel.agent.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int code;
    private String message;
    private String path;
    private long timestamp;

    public ErrorResponse(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = System.currentTimeMillis();
    }
}
