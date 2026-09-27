package com.devpilot.exception;

import org.springframework.http.HttpStatus;

public class GitHubException extends RuntimeException {

    private final HttpStatus status;

    public GitHubException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public GitHubException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
