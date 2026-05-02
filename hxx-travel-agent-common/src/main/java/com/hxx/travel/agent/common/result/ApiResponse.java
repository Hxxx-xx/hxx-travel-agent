package com.hxx.travel.agent.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应封装
 * 所有接口返回值统一使用此格式
 *
 * @author hxx
 */
@Data
public class ApiResponse<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("Success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(400);
        response.setCode(Integer.parseInt(code) > 0 ? Integer.parseInt(code) : 400);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
