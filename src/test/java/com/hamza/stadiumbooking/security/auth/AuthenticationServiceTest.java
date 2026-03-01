package com.hamza.stadiumbooking.security.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.security.jwt.JwtProvider;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private DecodedJWT decodedJWT;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private String refreshToken;
    private User user;
    private final String email = "hamza@gmail.com";

    @BeforeEach
    void setUp() {
        user = new User(
                UUID.randomUUID(), 0L, "hamza", email, "01000000000",
                "12345", LocalDate.now(), null, null, Role.ROLE_PLAYER, false
        );
        refreshToken = "valid.refresh.token";
    }

    @Test
    @DisplayName("Should successfully refresh token and return new access token")
    void refreshToken_HappyPath_ShouldSucceed() {
        given(jwtProvider.decodedJWT(refreshToken, "REFRESH")).willReturn(decodedJWT);
        given(decodedJWT.getSubject()).willReturn(email);
        given(userRepository.findByEmailAndIsDeletedFalse(email)).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(email, user.getId(),false, List.of(Role.ROLE_PLAYER.name()))).willReturn("new_access_token");

        AuthenticationResponse response = authenticationService.refreshToken(refreshToken);

        assertThat(response.accessToken()).isEqualTo("new_access_token");
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user in token does not exist")
    void refreshToken_UserNotFound_ShouldThrowException() {
        given(jwtProvider.decodedJWT(refreshToken, "REFRESH")).willReturn(decodedJWT);
        given(decodedJWT.getSubject()).willReturn(email);
        given(userRepository.findByEmailAndIsDeletedFalse(email)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refreshToken(refreshToken))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}