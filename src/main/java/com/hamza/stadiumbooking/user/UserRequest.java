package com.hamza.stadiumbooking.user;

import java.time.LocalDate;

public record UserRequest(
        String name,
        String email,
        String phoneNumber,
        String password,
        LocalDate dob
) {
}