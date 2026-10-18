package com.estacionamento.api.web.error;

import com.estacionamento.api.application.hikvision.HikvisionUnknownCameraException;
import com.estacionamento.api.application.shared.DuplicateResourceException;
import com.estacionamento.api.application.shared.InvalidAssignmentException;
import com.estacionamento.api.application.shared.ResourceNotFoundException;
import com.estacionamento.api.infrastructure.hikvision.HikvisionXmlParsingException;
import com.estacionamento.api.web.hikvision.HikvisionWebhookUnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid credentials", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleConflict(DuplicateResourceException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(org.springframework.dao.DataIntegrityViolationException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "The operation conflicts with an existing or related record", request);
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentUpdate(org.springframework.dao.OptimisticLockingFailureException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "The record changed concurrently; reload before retrying", request);
    }

    @ExceptionHandler(InvalidAssignmentException.class)
    public ResponseEntity<ApiError> handleInvalidAssignment(InvalidAssignmentException exception, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(HikvisionWebhookUnauthorizedException.class)
    public ResponseEntity<ApiError> handleHikvisionUnauthorized(HikvisionWebhookUnauthorizedException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler(HikvisionUnknownCameraException.class)
    public ResponseEntity<ApiError> handleUnknownHikvisionCamera(HikvisionUnknownCameraException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(HikvisionXmlParsingException.class)
    public ResponseEntity<ApiError> handleHikvisionXml(HikvisionXmlParsingException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }
}
