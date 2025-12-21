package com.hamza.stadiumbooking.exception;

// TODO: Refactor User Entity to add phone number aUnique Constraint
public class PhoneNumberTakenException extends RuntimeException{
    public PhoneNumberTakenException(String message) {
        super(message);
    }
}
