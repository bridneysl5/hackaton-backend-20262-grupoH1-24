package com.tuckersoft.branchengine.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String error;

    public ApiException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() { return status; }
    public String getError() { return error; }

    public static ApiException notFound(String msg)   { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", msg); }
    public static ApiException conflict(String msg)   { return new ApiException(HttpStatus.CONFLICT, "CONFLICT", msg); }
    public static ApiException badRequest(String msg) { return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg); }
    public static ApiException forbidden(String msg)  { return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", msg); }
    public static ApiException unauthorized(String msg){ return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", msg); }
}
