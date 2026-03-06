package com.hamza.stadiumbooking;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import com.hamza.stadiumbooking.base.AuthTestUtils;
import com.hamza.stadiumbooking.security.auth.LoginRequest;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import com.hamza.stadiumbooking.user.UserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @DisplayName("Authentication Integration Flow")
    class AuthenticationIT extends AbstractIntegrationTest {

        @Autowired
        private AuthTestUtils authUtils;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private static final String API_V1_USERS = "/api/v1/users";
        private static final String API_V1_LOGIN = "/api/v1/auth/login";
        private static final String API_V1_REFRESH_TOKEN = "/api/v1/auth/refresh-token";

        @Test
        @DisplayName("Should register a new user successfully")
        void register_ShouldCreateUser() throws Exception {
            // Given
            UserRequest request = authUtils.createUserRequest("hamza@gmail.com","01234567890");

            // When & Then
            mockMvc.perform(post(API_V1_USERS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isCreated());

            var savedUser = userRepository.findByEmailAndIsDeletedFalse(request.email()).orElseThrow();
            assertThat(passwordEncoder.matches(request.password(), savedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should return tokens upon valid login")
        void login_ShouldReturnTokens() throws Exception {
            authUtils.savePlayer("login@gmail.com", "Password@123","01111111111");
            LoginRequest loginRequest = new LoginRequest("login@gmail.com", "Password@123");

            // When & Then
            mockMvc.perform(post(API_V1_LOGIN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                    ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").exists())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true));
        }

        @Test
        @DisplayName("Should authorize access to user details with valid JWT")
        void getUserById_ShouldReturnUserDetails_WhenAuthenticated() throws Exception {
            // Given
            User user = authUtils.savePlayer("auth@gmail.com", "Password@123","01111111111");
            String token = authUtils.obtainAccessToken("auth@gmail.com", "Password@123");

            // When & Then
            mockMvc.perform(get(API_V1_USERS + "/{userId}", user.getId())
                            .header("Authorization", "Bearer " + token)
                    ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("auth@gmail.com"))
                    .andExpect(jsonPath("$.id").value(user.getId().toString()));
        }

        @Test
        @DisplayName("failed login should return 400 when Email and PassWord are not valid")
        void failedLogin_ShouldReturnBadRequest_whenEmailIsNotValid() throws Exception {
            LoginRequest loginRequest = new LoginRequest("hamza$gmail.com", "1234");

            mockMvc.perform(post(API_V1_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when password is incorrect")
        void login_ShouldReturn401_WhenPasswordIsWrong() throws Exception {
            // Given
            authUtils.savePlayer("test@gmail.com", "CorrectPassword@123","01111111111");
            LoginRequest loginRequest = new LoginRequest("test@gmail.com", "WrongPassword");

            // When & Then
            mockMvc.perform(post(API_V1_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when accessing protected resource without token")
        void getProtectedResource_ShouldReturn401_WhenNoToken() throws Exception {
            mockMvc.perform(get(API_V1_USERS + "/" + UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 409 when a player tries to use an duplicate email")
        void register_ShouldReturn409_WhenEmailIsDuplicate() throws Exception {
            authUtils.savePlayer("duplicate@gmail.com", "Pass@123", "01111111111");
            UserRequest request = authUtils.createUserRequest("duplicate@gmail.com", "01234567890");

            mockMvc.perform(post(API_V1_USERS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 409 when a player tries to use an duplicate phoneNumber")
        void register_ShouldReturn409_WhenPhoneNumberIsDuplicate() throws Exception {
            authUtils.savePlayer("email@gmail.com", "Pass@123", "01111111111");
            UserRequest request = authUtils.createUserRequest("anotherEmail@gmail.com", "01111111111");

            mockMvc.perform(post(API_V1_USERS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 401 when user is deleted")
        void login_ShouldReturn401_WhenUserIsDeleted() throws Exception {
            // Given
            User user = User.builder()
                    .name("deleted").email("deleted@gmail.com").password(passwordEncoder.encode("Password@123"))
                    .phoneNumber("12345678900").dob(LocalDate.of(1995, 1, 1)).role(Role.ROLE_PLAYER)
                    .isDeleted(true).build();
            userRepository.save(user);

            LoginRequest loginRequest = new LoginRequest("deleted@gmail.com","Password@123");

            // When & Then
            mockMvc.perform(post(API_V1_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 upon Malformed Access token")
        void getUserById_ShouldReturn401_WhenAccessTokenIsMalformed() throws Exception {
            User user = authUtils.savePlayer("login@gmail.com", "Password@123", "01111111111");
            String accessToken = authUtils.obtainAccessToken("login@gmail.com", "Password@123");
            String fakeToken = accessToken + " Fake";

            mockMvc.perform(get(API_V1_USERS + "/{userId}", user.getId())
                            .header("Authorization", "Bearer " + fakeToken)
                    ).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 upon Malformed Refresh token")
        void refreshToken_ShouldReturn401_WhenCookieIsMalformed() throws Exception {
            authUtils.savePlayer("refresh@gmail.com", "Password@123", "01111111111");
            jakarta.servlet.http.Cookie validRefreshToken =
                    authUtils.obtainRefreshToken("refresh@gmail.com", "Password@123");

            jakarta.servlet.http.Cookie malformedCookie =
                    new jakarta.servlet.http.Cookie("refresh_token", validRefreshToken.getValue() + "_fake");

            mockMvc.perform(post(API_V1_REFRESH_TOKEN)
                    .cookie(malformedCookie)
            ).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 when user wants to renew Access token with Refresh token")
        void refreshToken_ShouldReturnNewAccessToken() throws Exception {
            authUtils.savePlayer("refresh@gmail.com", "Password@123", "01111111111");
            jakarta.servlet.http.Cookie validRefreshToken =
                    authUtils.obtainRefreshToken("refresh@gmail.com", "Password@123");

            mockMvc.perform(post(API_V1_REFRESH_TOKEN)
                    .cookie(validRefreshToken)
            ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").exists());
        }

        @Test
        @DisplayName("Should return 400 when user dose not have refresh token in cookie")
        void refreshToken_ShouldThrowException_whenThereIsNoRefreshTokenInCookie() throws Exception {
            authUtils.savePlayer("refresh@gmail.com", "Password@123", "01111111111");
            mockMvc.perform(post(API_V1_REFRESH_TOKEN)
                    ).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when JWT is missing the mandatory 'id' claim")
        void getUserById_ShouldReturn401_WhenJwtIsMissingIdClaim() throws Exception {
            String tokenWithoutId = com.auth0.jwt.JWT.create()
                    .withSubject("hacker@gmail.com")
                    .withClaim("type", "ACCESS")
                    .withIssuer("stadium-booking-system")
                    .withClaim("roles", List.of("ROLE_PLAYER"))
                    // ❌ .withClaim("id", ...)
                    .withExpiresAt(new java.util.Date(System.currentTimeMillis() + 100000))
                    .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("40b024443198e9680327464047e62a22c15c0e768e7f12e96e5d8d9b8e2f3d1a".getBytes()));

            // 2. When & Then
            mockMvc.perform(get(API_V1_USERS + "/" + UUID.randomUUID())
                            .header("Authorization", "Bearer " + tokenWithoutId)
                    ).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.msg").value("Authentication failed: Invalid Token"));
        }
    }
