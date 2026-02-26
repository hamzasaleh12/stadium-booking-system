package com.hamza.stadiumbooking;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import com.hamza.stadiumbooking.security.auth.LoginRequest;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import com.hamza.stadiumbooking.user.UserRequest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

    @DisplayName("Authentication Integration Flow")
    class AuthenticationIT extends AbstractIntegrationTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private static final String API_V1_USERS = "/api/v1/users";
        private static final String API_V1_LOGIN = "/api/v1/login";

        @BeforeEach
        void cleanUp() {
            userRepository.deleteAll();
        }

        @Test
        @DisplayName("Should register a new user successfully")
        void register_ShouldCreateUser() throws Exception {
            // Given
            UserRequest request = createUserRequest("hamza@gmail.com","01234567890");

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
            saveTestUser("login@test.com", "Password@123");
            LoginRequest loginRequest = new LoginRequest("login@test.com", "Password@123");

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
            User user = saveTestUser("auth@test.com", "Password@123");
            String token = obtainAccessToken("auth@test.com", "Password@123");

            // When & Then
            mockMvc.perform(get(API_V1_USERS + "/{userId}", user.getId())
                            .header("Authorization", "Bearer " + token)
                    ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("auth@test.com"))
                    .andExpect(jsonPath("$.id").value(user.getId().toString()));
        }

        // --- Helper Methods ---

        private UserRequest createUserRequest(String email , String phoneNumber) {
            return new UserRequest("hamza", email, phoneNumber, "Hamza@1234", LocalDate.of(2000, 1, 1));
        }

        private User saveTestUser(String email, String rawPassword) {
            User user = User.builder()
                    .name("Test User")
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .phoneNumber("01111111111")
                    .dob(LocalDate.of(1995, 1, 1))
                    .role(Role.ROLE_PLAYER)
                    .isDeleted(false)
                    .build();
            return userRepository.save(user);
        }

        private String obtainAccessToken(String email, String password) throws Exception {
            LoginRequest loginRequest = new LoginRequest(email, password);
            MvcResult result = mockMvc.perform(post(API_V1_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn();
            return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
        }
    }