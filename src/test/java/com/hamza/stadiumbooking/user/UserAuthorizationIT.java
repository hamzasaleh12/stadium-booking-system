package com.hamza.stadiumbooking.user;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import com.hamza.stadiumbooking.base.AuthTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User Authorization Integration Tests")
class UserAuthorizationIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authUtils;

    private static final String API_V1_USERS = "/api/v1/users";

    private User player1;
    private User player2;
    private String adminToken;
    private String player1Token;

    @BeforeEach
    void setUp() throws Exception {
        // Setup Personas
        User admin = authUtils.saveUser("admin@gmail.com", "Admin@123", "01555555555", Role.ROLE_ADMIN);
        player1 = authUtils.saveUser("player1@gmail.com", "Pass@123", "01111111112", Role.ROLE_PLAYER);
        player2 = authUtils.saveUser("player2@gmail.com", "Pass@123", "01234567899", Role.ROLE_PLAYER);

        // Obtain Tokens
        adminToken = authUtils.obtainAccessToken(admin.getEmail(), "Admin@123");
        player1Token = authUtils.obtainAccessToken(player1.getEmail(), "Pass@123");
    }

    @Nested
    @DisplayName("Read Operations (GET)")
    class ReadOperations {

        @Test
        @DisplayName("Player can access their own profile (200 OK)")
        void playerCanAccessOwnProfile() throws Exception {
            mockMvc.perform(get(API_V1_USERS + "/{userId}", player1.getId())
                            .header("Authorization", "Bearer " + player1Token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Player cannot access another player's profile (403 Forbidden)")
        void playerCannotAccessOtherProfile() throws Exception {
            mockMvc.perform(get(API_V1_USERS + "/{userId}", player2.getId())
                            .header("Authorization", "Bearer " + player1Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin can access ANY player's profile (200 OK)")
        void adminCanAccessAnyProfile() throws Exception {
            mockMvc.perform(get(API_V1_USERS + "/{userId}", player1.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin can get ALL users (200 OK)")
        void adminCanGetAllUsers() throws Exception {
            mockMvc.perform(get(API_V1_USERS)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Player cannot get ALL users (403 Forbidden)")
        void playerCannotGetAllUsers() throws Exception {
            mockMvc.perform(get(API_V1_USERS)
                            .header("Authorization", "Bearer " + player1Token))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Update Operations (PUT)")
    class UpdateOperations {

        @Test
        @DisplayName("Player can update their own profile (200 OK)")
        void playerCanUpdateOwnProfile() throws Exception {
            UserUpdateRequest updateRequest = new UserUpdateRequest("Updated Name", "updated@gmail.com",
                    "01234567890","7368fiR$", LocalDate.of(1995, 1, 1));

            mockMvc.perform(put(API_V1_USERS + "/{userId}", player1.getId())
                            .header("Authorization", "Bearer " + player1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Player cannot update another player's profile (403 Forbidden)")
        void playerCannotUpdateOtherProfile() throws Exception {
            UserUpdateRequest updateRequest = new UserUpdateRequest("Hacked Name", "hacked@gmail.com",
                    "01111111111","Hack@1234",LocalDate.of(1995, 1, 1));

            mockMvc.perform(put(API_V1_USERS + "/{userId}", player2.getId())
                            .header("Authorization", "Bearer " + player1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin can update ANY player's profile (200 OK)")
        void adminCanUpdateAnyProfile() throws Exception {
            UserUpdateRequest updateRequest = new UserUpdateRequest("Admin Updated", "admin_up@gmail.com",
                    "01000000000", "Admin@123", LocalDate.of(1990, 1, 1));

            mockMvc.perform(put(API_V1_USERS + "/{userId}", player1.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Admin can change user role (200 OK & Role Updated)")
        void adminCanChangeRole() throws Exception {
            mockMvc.perform(put(API_V1_USERS + "/{userId}/role", player1.getId())
                            .param("roleAsString", "ROLE_MANAGER")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ROLE_MANAGER"));
        }

        @Test
        @DisplayName("Player cannot change user role (403 Forbidden)")
        void playerCannotChangeRole() throws Exception {
            mockMvc.perform(put(API_V1_USERS + "/{userId}/role", player1.getId())
                            .param("roleAsString", "ROLE_ADMIN")
                            .header("Authorization", "Bearer " + player1Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin receives 400 Bad Request when providing invalid role")
        void adminCannotChangeToInvalidRole() throws Exception {
            mockMvc.perform(put(API_V1_USERS + "/{userId}/role", player1.getId())
                            .param("roleAsString", "ROLE_BATMAN")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Delete Operations (DELETE)")
    class DeleteOperations {

        @Test
        @DisplayName("Player can delete their own profile (204 No Content)")
        void playerCanDeleteOwnProfile() throws Exception {
            mockMvc.perform(delete(API_V1_USERS + "/{userId}", player1.getId())
                            .header("Authorization", "Bearer " + player1Token))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Player cannot delete another player's profile (403 Forbidden)")
        void playerCannotDeleteOtherProfile() throws Exception {
            mockMvc.perform(delete(API_V1_USERS + "/{userId}", player2.getId())
                            .header("Authorization", "Bearer " + player1Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin can delete ANY player's profile (204 No Content)")
        void adminCanDeleteAnyProfile() throws Exception {
            mockMvc.perform(delete(API_V1_USERS + "/{userId}", player1.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }
    }
}
