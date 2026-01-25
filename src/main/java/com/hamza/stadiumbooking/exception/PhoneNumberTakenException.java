package com.hamza.stadiumbooking.exception;

public class PhoneNumberTakenException extends RuntimeException{
    public PhoneNumberTakenException(String message) {
        super(message);
    }
}
