package com.pundix.wallet.task.utils;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ApiResponse implements Serializable {

    private int status;
    private String message;
    private Object data;

    public ApiResponse(int code, String message, Object data) {
        this.status = code;
        this.data = data;
        this.message = message;
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(200, message, data);
    }

    public static ApiResponse error(int status, String message) {
        return new ApiResponse(status, message, null);
    }

}
