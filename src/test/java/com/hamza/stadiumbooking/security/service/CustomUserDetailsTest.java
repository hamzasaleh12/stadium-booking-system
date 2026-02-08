package com.hamza.stadiumbooking.security.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsTest {

    private CustomUserDetails userDetails;

    @Test
    void isEnabled_ShouldReturnFalse_WhenUserIsDeleted() {
        userDetails = new CustomUserDetails(null, "", "", true, null);
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_ShouldReturnTrue_WhenUserIsNotDeleted() {
        userDetails = new CustomUserDetails(null, "", "", false, null);
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void shouldReturnTrueForDefaultSecurityMethods() {
        userDetails = new CustomUserDetails(null, "user", "pass", false, null);

        assertAll("Security Flags",
                () -> assertThat(userDetails.isAccountNonExpired()).isTrue(),
                () -> assertThat(userDetails.isAccountNonLocked()).isTrue(),
                () -> assertThat(userDetails.isCredentialsNonExpired()).isTrue()
        );
    }
}