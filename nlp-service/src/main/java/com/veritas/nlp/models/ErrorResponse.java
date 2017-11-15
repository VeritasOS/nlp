package com.veritas.nlp.models;

import com.veritas.nlp.resources.ErrorCode;

public class ErrorResponse {
    private int statusCode;
    private String message;
    private ErrorCode error;

    public ErrorResponse() {
    }

    public ErrorResponse(int statusCode, ErrorCode error, String message) {
        this.statusCode = statusCode;
        this.error = error;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}
