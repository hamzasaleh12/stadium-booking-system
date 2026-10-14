package com.hamza.stadiumbooking.user.dto;

import com.hamza.stadiumbooking.user.Role;
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
