package com.tyut.studyroombookingcheckin.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> success() {
        return build(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return build(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return build(200, message, data);
    }

    public static <T> Result<T> error(String message) {
        return build(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return build(code, message, null);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> build(Integer code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}