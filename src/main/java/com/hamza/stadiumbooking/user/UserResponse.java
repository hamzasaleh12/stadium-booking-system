package com.hamza.stadiumbooking.user;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        Role role,
        Integer age
) {
}
