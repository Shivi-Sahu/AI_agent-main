package com.devpilot.exception;

import org.springframework.http.HttpStatus;

public class AgentException extends RuntimeException {

    private final HttpStatus status;

    public AgentException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public AgentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public AgentException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
