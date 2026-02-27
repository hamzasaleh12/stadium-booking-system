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
import java.util.UUID;

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
        private static final String API_V1_LOGIN = "/api/v1/auth/login";

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
            saveTestUser("login@gmail.com", "Password@123","01111111111");
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
            User user = saveTestUser("auth@gmail.com", "Password@123","01111111111");
            String token = obtainAccessToken("auth@gmail.com", "Password@123");

            // When & Then
            mockMvc.perform(get(API_V1_USERS + "/{userId}", user.getId())
                            .header("Authorization", "Bearer " + token)
                    ).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("auth@gmail.com"))
                    .andExpect(jsonPath("$.id").value(user.getId().toString()));
        }

        @Test
        void failedLogin_ShouldReturnBadRequest_whenEmailIsNotValid() throws Exception {
            LoginRequest loginRequest = new LoginRequest("hamza$gmail.com", "1234");

            MvcResult result = mockMvc.perform(post(API_V1_LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest))
            ).andReturn();

            assertThat(result.getResponse().getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("Should return 401 when password is incorrect")
        void login_ShouldReturn401_WhenPasswordIsWrong() throws Exception {
            // Given
            saveTestUser("test@gmail.com", "CorrectPassword@123","01111111111");
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
        @DisplayName("Should return 403 when a player tries to access another player's profile")
        void getUserById_ShouldReturn403_WhenAccessingDifferentPlayerProfile() throws Exception {
            User player1 = saveTestUser("player1@gmail.com", "Pass@123","01111111111");
            User player2 = saveTestUser("player2@gmail.com", "Pass@123","01234567890");

            String token1 = obtainAccessToken(player1.getEmail(), "Pass@123");

            mockMvc.perform(get(API_V1_USERS + "/{userId}", player2.getId())
                            .header("Authorization", "Bearer " + token1)
                    ).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.httpStatus").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.zonedDateTime").exists());
        }

        // --- Helper Methods ---

        private UserRequest createUserRequest(String email , String phoneNumber) {
            return new UserRequest("hamza", email, phoneNumber, "Hamza@1234", LocalDate.of(2000, 1, 1));
        }

        private User saveTestUser(String email, String rawPassword , String phoneNumber) {
            User user = User.builder()
                    .name("Test User")
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .phoneNumber(phoneNumber)
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