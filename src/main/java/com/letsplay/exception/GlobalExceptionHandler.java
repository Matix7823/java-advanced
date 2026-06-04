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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@SuppressWarnings("null")
public class GlobalExceptionHandler {

    // ── Erreurs de validation (400) ───────────────────────────────────────────

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

    // ── Erreurs liées à la logique métier ─────────────────────────────────────

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

    // ── Erreurs Spring Security ───────────────────────────────────────────────

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(RuntimeException ex) {
        // Intentionnellement flou — on ne révèle pas si l'email existe ou non
        return build(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Vous n'avez pas l'autorisation d'effectuer cette action", null);
    }

    // ── Erreurs Client (4xx) ─────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed: " + ex.getMethod() + ". Use " + ex.getSupportedHttpMethods(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Required request body is missing or malformed", null);
    }

    // ── Solution de repli (catch-all — évite de fuiter des stack traces en 5xx) ─

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue", null);
    }

    // ── Méthodes utilitaires ──────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                 Map<String, String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), message, details, Instant.now().toString()));
    }

    public record ErrorResponse(int status, String message,
                                Map<String, String> details, String timestamp) {}
}
