package com.hamza.stadiumbooking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

@Getter
public class ApiError {
    private final String msg;
    private final HttpStatus httpStatus;
    private final ZonedDateTime zonedDateTime;

    public ApiError(String msg, HttpStatus httpStatus, ZonedDateTime zonedDateTime) {
        this.msg = msg;
        this.httpStatus = httpStatus;
        this.zonedDateTime = zonedDateTime;
    }

}
