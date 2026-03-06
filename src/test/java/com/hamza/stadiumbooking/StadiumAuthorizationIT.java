package com.hamza.stadiumbooking;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import com.hamza.stadiumbooking.base.AuthTestUtils;
import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.stadium.StadiumRequest;
import com.hamza.stadiumbooking.stadium.StadiumRequestForUpdate;
import com.hamza.stadiumbooking.stadium.Type;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Stadium Authorization & Ownership Integration Tests")
class StadiumAuthorizationIT extends AbstractIntegrationTest {

    @MockitoSpyBean private StadiumRepository stadiumRepository;

    @Autowired private UserRepository userRepository;
    @Autowired private AuthTestUtils authUtils;

    private static final String API_V1_STADIUMS = "/api/v1/stadiums";

    private String adminToken, manager1Token, manager2Token, playerToken;
    private UUID stadium1Id; // Owned by Manager 1

    @BeforeEach
    void setUp() throws Exception {
        stadiumRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Setup Personas
        User admin = authUtils.saveUser("admin@gmail.com", "Admin@123", "01034567890", Role.ROLE_ADMIN);
        User m1 = authUtils.saveUser("m1@gmail.com", "Pass@123", "01134567890", Role.ROLE_MANAGER);
        User m2 = authUtils.saveUser("m2@gmail.com", "Pass@123", "01234567890", Role.ROLE_MANAGER);
        User p1 = authUtils.savePlayer("p1@gmail.com", "Pass@123", "01534567890");

        // 2. Obtain Tokens
        adminToken = authUtils.obtainAccessToken(admin.getEmail(), "Admin@123");
        manager1Token = authUtils.obtainAccessToken(m1.getEmail(), "Pass@123");
        manager2Token = authUtils.obtainAccessToken(m2.getEmail(), "Pass@123");
        playerToken = authUtils.obtainAccessToken(p1.getEmail(), "Pass@123");

        // 3. Setup Initial Stadium for Ownership Tests
        Stadium stadium = Stadium.builder()
                .name("Anfield")
                .location("Liverpool")
                .pricePerHour(500.0)
                .type(Type.SEVEN_A_SIDE)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .photoUrl("https://photo.com")
                .owner(m1)
                .build();
        stadium1Id = stadiumRepository.save(stadium).getId();
    }

    @Nested
    @DisplayName("Read Operations (GET - Public)")
    class ReadOperations {
        @Test
        @DisplayName("Anonymous user can get all stadiums (200 OK)")
        void anonymousCanGetAllStadiums() throws Exception {
            mockMvc.perform(get(API_V1_STADIUMS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").exists());
        }

        @Test
        @DisplayName("Anonymous user can get stadium by ID (200 OK)")
        void anonymousCanGetStadiumById() throws Exception {
            mockMvc.perform(get(API_V1_STADIUMS + "/{id}", stadium1Id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(stadium1Id.toString()));
        }
    }

    @Nested
    @DisplayName("Create Operations (POST)")
    class CreateOperations {

        @Test
        @DisplayName("Admin can create stadium (201 Created)")
        void adminCanCreateStadium() throws Exception {
            StadiumRequest request = authUtils.createStadiumRequest("Camp Nou");
            mockMvc.perform(post(API_V1_STADIUMS)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Manager can create stadium (201 Created)")
        void managerCanCreateStadium() throws Exception {
            StadiumRequest request = authUtils.createStadiumRequest("Camp Nou");
            mockMvc.perform(post(API_V1_STADIUMS)
                            .header("Authorization", "Bearer " + manager1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Player cannot create stadium (403 Forbidden)")
        void playerCannotCreateStadium() throws Exception {
            StadiumRequest request = authUtils.createStadiumRequest("Forbidden");
            mockMvc.perform(post(API_V1_STADIUMS)
                            .header("Authorization", "Bearer " + playerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Update & Ownership (PUT)")
    class UpdateOperations {
        @Test
        @DisplayName("Owner Manager can update their own stadium (200 OK)")
        void ownerCanUpdateStadium() throws Exception {
            StadiumRequestForUpdate update = new StadiumRequestForUpdate("Updated Anfield", 600.0, 60, null, null, null, null);
            mockMvc.perform(put(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + manager1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Non-Owner Manager cannot update other's stadium (403 Forbidden)")
        void nonOwnerCannotUpdateStadium() throws Exception {
            StadiumRequestForUpdate update = new StadiumRequestForUpdate("Hacked", 1.0, 0, null, null, null, null);
            mockMvc.perform(put(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + manager2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin can update ANY stadium (200 OK)")
        void adminCanUpdateAnyStadium() throws Exception {
            StadiumRequestForUpdate update = new StadiumRequestForUpdate("Admin Override", 700.0, 70, null, null, null, null);
            mockMvc.perform(put(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(update)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Player cannot delete stadium (403 Forbidden)")
        void playerCannotUpdateStadium() throws Exception {
            mockMvc.perform(put(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + playerToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Delete Operations (DELETE)")
    class DeleteOperations {
        @Test
        @DisplayName("Admin can soft-delete their stadium (204 No Content)")
        void adminCanDeleteStadium() throws Exception {
            mockMvc.perform(delete(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Owner can soft-delete their stadium (204 No Content)")
        void ownerCanDeleteStadium() throws Exception {
            mockMvc.perform(delete(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + manager1Token))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Non-Owner Manager cannot delete other's stadium (403 Forbidden)")
        void nonOwnerCannotDeleteStadium() throws Exception {
            mockMvc.perform(delete(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + manager2Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Player cannot delete stadium (403 Forbidden)")
        void playerCannotDeleteStadium() throws Exception {
            mockMvc.perform(delete(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + playerToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Stadium Flow & Integrity Tests")
    class StadiumFlowTests {

        @Test
        @DisplayName("Verify Full Cache Lifecycle: Hit before Update & Evict after Update")
        void verifyFullCacheLifecycle() throws Exception {
            // 1. First GET: Database hit (Call 1)
            mockMvc.perform(get(API_V1_STADIUMS + "/{id}", stadium1Id)).andExpect(status().isOk());

            // 2. Second GET: Cache hit - No DB interaction (Still Call 1)
            mockMvc.perform(get(API_V1_STADIUMS + "/{id}", stadium1Id)).andExpect(status().isOk());

            verify(stadiumRepository, times(1)).findByIdAndIsDeletedFalse(stadium1Id);

            // 3. Update the stadium: Inside updateStadium method (Call 2)
            StadiumRequestForUpdate updateRequest = new StadiumRequestForUpdate(
                    "Anfield New Era", 1000.0, 150, null, null, null, "https://new-anfield.com"
            );

            mockMvc.perform(put(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + manager1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            // 4. Third GET: Database hit because @CacheEvict cleared the cache (Call 3)
            mockMvc.perform(get(API_V1_STADIUMS + "/{id}", stadium1Id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Anfield New Era"));

            verify(stadiumRepository, times(3)).findByIdAndIsDeletedFalse(stadium1Id);
        }

        @Test
        @DisplayName("Input Validation: Should return 400 for negative price")
        void shouldReturn400ForNegativePrice() throws Exception {
            StadiumRequest invalidRequest = new StadiumRequest(
                    "Invalid Stadium", "Cairo", -100.0, -10, // قيم سالبة مرفوضة
                    LocalTime.of(8,0), LocalTime.of(22,0), null, Type.FIVE_A_SIDE, "https://photo.com"
            );

            mockMvc.perform(post(API_V1_STADIUMS)
                            .header("Authorization", "Bearer " + manager1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value("Validation Failed"))
                    .andExpect(jsonPath("$.validationErrors.pricePerHour").exists());
        }

        @Test
        @DisplayName("Soft-Delete Integrity: Deleted stadium must not be accessible")
        void deletedStadiumShouldNotBeAccessible() throws Exception {
            mockMvc.perform(delete(API_V1_STADIUMS + "/{id}", stadium1Id)
                            .header("Authorization", "Bearer " + manager1Token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(API_V1_STADIUMS + "/{id}", stadium1Id))
                    .andExpect(status().isNotFound());

            // 3. Check getAllStadiums list
            mockMvc.perform(get(API_V1_STADIUMS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[?(@.id == '%s')]", stadium1Id.toString()).doesNotExist());
        }

        @Test
        @DisplayName("Verify locations are cached correctly")
        void verifyLocationsCaching() throws Exception {
            mockMvc.perform(get(API_V1_STADIUMS + "/locations")).andExpect(status().isOk());
            mockMvc.perform(get(API_V1_STADIUMS + "/locations")).andExpect(status().isOk());

            verify(stadiumRepository, times(1)).findAllDistinctLocations();
        }
    }
}
