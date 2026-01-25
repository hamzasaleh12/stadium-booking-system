package com.hamza.stadiumbooking.user;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String phoneNumber,
        Role role,
        Integer age
) {
}
