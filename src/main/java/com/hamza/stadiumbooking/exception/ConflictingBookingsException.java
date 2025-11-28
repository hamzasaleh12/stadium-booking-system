package com.hamza.stadiumbooking.exception;

public class ConflictingBookingsException extends RuntimeException{
    public ConflictingBookingsException(String message) {
        super(message);
    }
}
