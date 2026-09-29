package com.hamza.stadiumbooking.exception;

public class IpBannedException extends RuntimeException {
    public IpBannedException() {
        super("Requests from this IP address are temporarily blocked.");
    }
}
