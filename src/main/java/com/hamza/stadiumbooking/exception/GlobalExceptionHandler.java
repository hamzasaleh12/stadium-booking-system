package com.hamza.stadiumbooking.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException e) {
        return error(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException e) {
        return error(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandler() {
        return error("Endpoint not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmailTakenException.class)
    public ResponseEntity<Object> handleEmailTakenException(EmailTakenException e) {
        return error(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PhoneNumberTakenException.class)
    public ResponseEntity<Object> handlePhoneTakenException(PhoneNumberTakenException e) {
        return error(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ConflictingBookingsException.class)
    public ResponseEntity<Object> handleConflictingBookingsException(ConflictingBookingsException e) {
        return error(e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Object> handleRateLimitExceeded(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(e.getRetryAfterSeconds()))
                .body(new ApiError("Too many requests. Please try again later.",
                        HttpStatus.TOO_MANY_REQUESTS, ZonedDateTime.now()));
    }

    @ExceptionHandler(IpBannedException.class)
    public ResponseEntity<Object> handleIpBanned(IpBannedException e) {
        return error(e.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({JWTVerificationException.class, TokenExpiredException.class})
    public ResponseEntity<Object> handleJwtErrors(Exception e) {
        log.warn("JWT authentication failed", e);
        return error("Authentication failed", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Object> handleInvalidRefreshToken() {
        return error("Authentication failed", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<Object> handleMissingCookie(MissingRequestCookieException ex) {
        return error("Missing cookie: " + ex.getCookieName(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuth(AuthenticationException e) {
        log.warn("Authentication failed {}", e.getMessage());
        return error("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied {}", e.getMessage());
        return error("Access Denied: You don't have permission to perform this action.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        return error("Missing parameter: " + ex.getParameterName() + " is required.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException e) {
        return error("Resource not found: " + e.getResourcePath(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<Object> handleMissingPathVar(MissingPathVariableException ex) {
        return error("Missing path variable: " + ex.getVariableName(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> error instanceof FieldError fe ? fe.getField() : "msg",
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing + " | " + replacement
                ));

        return error("Validation Failed", HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler({ConstraintViolationException.class, WebExchangeBindException.class})
    public ResponseEntity<Object> handleConstraintViolation(Exception e) {
        return error("Validation failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error("Type mismatch for parameter: " + e.getName(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleBadJson(HttpMessageNotReadableException e) {
        log.warn("Bad JSON request {}", e.getMessage());
        return error("Malformed JSON request: " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return error("Unsupported media type: " + e.getContentType(), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Object> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return error("Not acceptable: " + e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler({
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<Object> handleConcurrencyConflicts(Exception e) {
        log.warn("Concurrency conflict or lock failure: {}", e.getMessage());
        return error("The record was updated by another user. Please refresh and try again.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation {}", e.getMessage());
        return error("Data integrity violation", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccess(DataAccessException e) {
        log.error("Database error: ", e);
        return error("Service temporarily unavailable due to database error.", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return error(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException(IllegalStateException e) {
        return error(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception e, WebRequest request) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        log.error("""
        ❌ [UNHANDLED ERROR REPORT]
        ---------------------------------------------------------
        Path: {}
        Exception Type: {}
        Culprit (Root Cause): {}
        Error Message: {}
        ---------------------------------------------------------
        """,
                request.getDescription(false),
                e.getClass().getSimpleName(),
                rootCause.getClass().getName(),
                rootCause.getMessage(),
                e
        );

        return error("An unexpected error occurred. Please try again later or contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> error(String msg, HttpStatus status){
        ApiError apiError = new ApiError(msg, status, ZonedDateTime.now());
        return new ResponseEntity<>(apiError, status);
    }
    private ResponseEntity<Object> error(String msg, HttpStatus status, Map<String, String> validationErrors) {
        ApiError apiError = new ApiError(msg, status, ZonedDateTime.now(), validationErrors);
        return new ResponseEntity<>(apiError, status);
    }
}
