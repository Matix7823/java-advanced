package com.letsplay.exception;

import com.letsplay.exception.ApiExceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation errors (400) ───────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    // ── Business logic errors ─────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EmailAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenActionException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    // ── Spring Security errors ────────────────────────────────────────────────

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
        // Intentionally vague — do not leak whether email exists
        return build(HttpStatus.UNAUTHORIZED, "Invalid email or password", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "You don't have permission to perform this action", null);
    }

    // ── Fallback (catch-all — prevents 5xx leaking stack traces) ─────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                 Map<String, String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, details, Instant.now().toString()));
    }

    public record ErrorResponse(int status, String message,
                                Map<String, String> details, String timestamp) {}
}
