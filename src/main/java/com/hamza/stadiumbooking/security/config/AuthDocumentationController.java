package com.hamza.stadiumbooking.security.config;

import com.hamza.stadiumbooking.security.auth.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthDocumentationController {

    @Operation(summary = "Login to get JWT Token")
    @PostMapping("/login")
    public void login(@RequestBody LoginRequest loginRequest) {
    }
}