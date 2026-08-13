package com.gymams.exception;

import org.springframework.http.HttpStatus;

/** Thrown anywhere in the service layer for an expected, user-facing failure. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
