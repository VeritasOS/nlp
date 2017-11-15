package com.veritas.nlp.resources;

public enum ErrorCode {
    SERVER_ERROR("Server error"),
    CLIENT_ERROR("Client error"),
    CONTENT_TOO_LARGE("Content is too large"),
    TIMEOUT("Operation took too long"),
    ENTITY_RECOGNITION_FAILED("Entity recognition failed");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
