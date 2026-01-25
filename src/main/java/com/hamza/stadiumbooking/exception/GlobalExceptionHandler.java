package com.hamza.stadiumbooking.exception;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

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

    @ExceptionHandler({JWTVerificationException.class, TokenExpiredException.class})
    public ResponseEntity<Object> handleJwtErrors(Exception e) {
        log.warn("JWT auth failed", e);
        return error("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuth(AuthenticationException e) {
        log.warn("Authentication failed", e);
        return error("Authentication failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied", e);
        return error("Access Denied: You don't have permission to perform this action.", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        return error("Missing parameter: " + ex.getParameterName() + " is required.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<Object> handleMissingPathVar(MissingPathVariableException ex) {
        return error("Missing path variable: " + ex.getVariableName(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ApiError apiError = new ApiError("Validation Failed", HttpStatus.BAD_REQUEST, ZonedDateTime.now(), errors);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
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
        log.warn("Bad JSON request", e);
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

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleConcurrencyFailure(ObjectOptimisticLockingFailureException e) {
        return error("The data was updated by another user. Please refresh and try again , for details: " + e.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return error("Data integrity violation", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccess(DataAccessException e) {
        log.error("Database access error", e);
        return error("Database error: " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
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
    public ResponseEntity<Object> handleGlobalException(Exception e) {
        log.error("Unhandled error: ", e);
        return error("An unexpected error occurred. Please try again later or contact support.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Object> error(String msg, HttpStatus status){
        ApiError apiError = new ApiError(msg, status, ZonedDateTime.now());
        return new ResponseEntity<>(apiError, status);
    }
}
