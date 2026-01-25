package com.hamza.stadiumbooking.exception;
import org.springframework.http.HttpStatus;
import java.time.ZonedDateTime;
import java.util.Map;

public record ApiError(
        String msg,
        HttpStatus httpStatus,
        ZonedDateTime zonedDateTime,
        Map<String, String> validationErrors
) {
    public ApiError(String msg, HttpStatus httpStatus, ZonedDateTime zonedDateTime) {
        this(msg, httpStatus, zonedDateTime, null);
    }
}
