package com.veritas.nlp.ner;

import com.veritas.nlp.resources.ErrorCode;

@SuppressWarnings("serial")
public class NerException extends Exception {
    private final ErrorCode code;

    public NerException(String message) {
        super(message);
        code = ErrorCode.SERVER_ERROR;
    }

    public NerException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public NerException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public NerException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public NerException(String message, Throwable cause) {
        super(message, cause);
        code = ErrorCode.SERVER_ERROR;
    }

    public ErrorCode getCode() {
        return code;
    }
}
