package com.letsplay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public final class ApiExceptions {

    private ApiExceptions() {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class ForbiddenActionException extends RuntimeException {
        public ForbiddenActionException(String message) { super(message); }
    }
}
