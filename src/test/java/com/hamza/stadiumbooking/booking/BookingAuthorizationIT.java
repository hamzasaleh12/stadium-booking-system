package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import com.hamza.stadiumbooking.base.AuthTestUtils;
import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@DisplayName("Booking Authorization & Ownership Integration Tests ")
public class BookingAuthorizationIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authUtils;

    @Autowired
    protected StadiumRepository stadiumRepository;

    @Autowired
    protected BookingRepository bookingRepository;

    private String adminToken;
    private String m1Token;
    private String m2Token;
    private String p1Token, p2Token;
    private UUID stadium1Id, stadium2Id;
    private UUID bookingP1Id;
    private UUID bookingP2Id;
    private User player1, player2;
    private final String BASE_URL = "/api/v1/bookings";

    @BeforeEach
    void setUp() throws Exception {
        // 1. (Personas)
        User admin = authUtils.saveUser("admin@gmail.com", "Admin@123", "01000000001", Role.ROLE_ADMIN);
        User m1 = authUtils.saveUser("m1@gmail.com", "Pass@123", "01000000002", Role.ROLE_MANAGER);
        User m2 = authUtils.saveUser("m2@gmail.com", "Pass@123", "01000000003", Role.ROLE_MANAGER);
        player1 = authUtils.savePlayer("p1@gmail.com", "Pass@123", "01000000004");
        player2 = authUtils.savePlayer("p2@gmail.com", "Pass@123", "01000000005");

        // 2. Keys (Tokens)
        adminToken = authUtils.obtainAccessToken(admin.getEmail(), "Admin@123");
        m1Token = authUtils.obtainAccessToken(m1.getEmail(), "Pass@123");
        m2Token = authUtils.obtainAccessToken(m2.getEmail(), "Pass@123");
        p1Token = authUtils.obtainAccessToken(player1.getEmail(), "Pass@123");
        p2Token = authUtils.obtainAccessToken(player2.getEmail(), "Pass@123");

        // 3. (Stadiums)
        Stadium s1 = authUtils.saveStadium("Anfield", m1); // Manager 1 owns this
        Stadium s2 = authUtils.saveStadium("Old Trafford", m2); // Manager 2 owns this
        stadium1Id = s1.getId();
        stadium2Id = s2.getId();

        // 4. (Bookings)
        bookingP1Id = authUtils.createAndSaveBooking(s1, player1, 2, 18).getId(); // P1 in Stadium 1
        bookingP2Id = authUtils.createAndSaveBooking(s2, player2, 3, 20).getId(); // P2 in Stadium 2
    }

    @Nested
    @DisplayName("Read Operations - The 17 Defensive Scenarios")
    class ReadOperations {

        // ================= ADMIN BLOCK (5 Scenarios) =================
        @Test
        @DisplayName("1. Admin: Global View (200 OK)")
        void adminGlobal() throws Exception {
            mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("2. Admin: Any Stadium List (200 OK)")
        void adminStadiumList() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stadiums/{sId}", stadium1Id).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("3. Admin: Any Player History (200 OK)")
        void adminPlayerHistory() throws Exception {
            mockMvc.perform(get(BASE_URL + "/players/{pId}", player1.getId()).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("4. Admin: Player in Stadium Filter (200 OK)")
        void adminFilter() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stadiums/{sId}/players/{pId}", stadium1Id, player1.getId()).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("5. Admin: Any ID Access (200 OK)")
        void adminById() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", bookingP1Id).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        }

        // ================= MANAGER BLOCK (6 Scenarios) =================
        @Test
        @DisplayName("6. Manager: Own Stadium List (200 OK)")
        void managerOwnStadium() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stadiums/{sId}", stadium1Id).header("Authorization", "Bearer " + m1Token)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("7. Manager: Own Stadium Booking ID (200 OK)")
        void managerOwnId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", bookingP1Id).header("Authorization", "Bearer " + m1Token)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("8. Manager: Player in THEIR stadium (200 OK)")
        void managerPlayerInOwn() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stadiums/{sId}/players/{pId}", stadium1Id, player1.getId()).header("Authorization", "Bearer " + m1Token)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("9. Manager: Global List (403 Forbidden)")
        void managerGlobalDenied() throws Exception {
            mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer " + m1Token)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("10. Manager: Neighbor Stadium List (403 Forbidden) - SCENARIO 17")
        void managerNeighborDenied() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stadiums/{sId}", stadium1Id).header("Authorization", "Bearer " + m2Token)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("11. Manager: Other Stadium ID (BOLA 403)")
        void managerBolaDenied() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", bookingP1Id).header("Authorization", "Bearer " + m2Token)).andExpect(status().isForbidden());
        }

        // ================= PLAYER BLOCK (5 Scenarios) =================
        @Test
        @DisplayName("12. Player: Own History (200 OK)")
        void playerOwnHistory() throws Exception {
            mockMvc.perform(get(BASE_URL + "/my-bookings").header("Authorization", "Bearer " + p1Token)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("13. Player: Own Booking ID (200 OK)")
        void playerOwnId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", bookingP1Id).header("Authorization", "Bearer " + p1Token)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("14. Player: Other Player ID (BOLA 403)")
        void playerBolaDenied() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", bookingP2Id).header("Authorization", "Bearer " + p1Token)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("15. Player: Global/Stadium Lists (403 Forbidden)")
        void playerListsDenied() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stadiums/{sId}", stadium1Id).header("Authorization", "Bearer " + p1Token)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("16. Player: Other Player Global History (403 Forbidden)")
        void playerOtherHistoryDenied() throws Exception {
            mockMvc.perform(get(BASE_URL + "/players/{pId}", player2.getId()).header("Authorization", "Bearer " + p1Token)).andExpect(status().isForbidden());
        }

        // ================= ANONYMOUS BLOCK (1 Scenario) =================
        @Test
        @DisplayName("17. Anonymous: Any ID Access (401 Unauthorized)")
        void anonymousDenied() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", bookingP1Id)).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Creation Operations (POST - Add Booking)")
    class CreationOperations {

        // ================= SECURITY & ROLES =================

        @Test
        @DisplayName("Anonymous: Should return 401 Unauthorized")
        void add_Anonymous_401() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 5, 18, 0, 120);
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Manager: Should return 403 Forbidden")
        void add_Manager_403() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 5, 18, 0, 120);
            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + m1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        // ================= VALIDATIONS =================

        @Test
        @DisplayName("Validation: Closed Stadium (400 Bad Request)")
        void add_ClosedStadium_400() throws Exception {
            // The stadium closes at 11 PM, we'll book at 2 AM
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 5, 2, 0, 60);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("Stadium is closed")));
        }

        @Test
        @DisplayName("Validation: Invalid Duration (Less than 1 hour - 400)")
        void add_ShortDuration_400() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 5, 18, 0, 30); // 30 mins

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Business Rule: Conflict with existing booking (409)")
        void add_Conflict_409() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 2, 18, 30, 60);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Validation: Booking in the past (400)")
        void add_PastDate_400() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, -1, 10, 0, 60);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Validation: Duration exceeds 3 hours (400)")
        void add_LongDuration_400() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 5, 10, 0, 240); // 4 hours

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Validation: Invalid increment (e.g., 70 mins - 400)")
        void add_InvalidIncrement_400() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 5, 10, 0, 70);

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        // ================= CONCURRENCY (The Race Condition Test) =================

        @Test
        @DisplayName("Concurrency: 500 Users booking same time (Virtual Threads Style)")
        void add_Concurrency_Test_Modern() throws Exception {
            int numberOfThreads = 500;

            var successCount = new java.util.concurrent.atomic.AtomicInteger(0);
            var failCount = new java.util.concurrent.atomic.AtomicInteger(0);
            var otherErrorCount = new java.util.concurrent.atomic.AtomicInteger(0);

            var latch = new java.util.concurrent.CountDownLatch(1);

            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 10, 19, 0, 60);
            String requestJson = objectMapper.writeValueAsString(request);

            try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                java.util.stream.IntStream.range(0, numberOfThreads).forEach(i ->
                        executor.submit(() -> {
                            try {
                                latch.await();
                                var result = mockMvc.perform(post(BASE_URL)
                                        .header("Authorization", "Bearer " + p1Token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson)).andReturn();

                                var status = result.getResponse().getStatus();

                                if (status == 201) {
                                    successCount.incrementAndGet();
                                } else if (status == 409) {
                                    failCount.incrementAndGet();
                                } else {
                                    otherErrorCount.incrementAndGet();
                                    log.error("❌ Error detected! Status: {} | Thread: {} | Response: {}", status, Thread.currentThread().getName(), result.getResponse().getContentAsString());
                                    if (result.getResolvedException() != null) {
                                        log.error("⚠ Root Cause: {}", result.getResolvedException().getMessage());
                                    }
                                }

                            } catch (Exception e) {
                                Thread.currentThread().interrupt();
                            }
                        })
                );

                latch.countDown();
            }

            assertThat(successCount.get()).isEqualTo(1);
            assertThat(failCount.get()).isEqualTo(numberOfThreads - 1);
        }

        // ================= SUCCESS PATH =================

        @Test
        @DisplayName("Success: Player can book and price is calculated correctly")
        void add_Success_201() throws Exception {
            BookingRequest request = authUtils.createBookingRequest(stadium1Id, 7, 20, 0, 120); // 2 hours

            mockMvc.perform(post(BASE_URL)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.totalPrice").value(450.0)) // 2*200 + 50 fee [cite: 2026-03-08]
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }
    }

    @Nested
    @DisplayName("Delete Operations (Policy & Security)")
    class DeleteOperations {

        // ================= SECURITY =================

        @Test
        @DisplayName("1. Player: Cannot cancel others booking (403)")
        void playerCannotCancelOthersBooking() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", bookingP2Id)
                            .header("Authorization", "Bearer " + p1Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("5. Manager: CANNOT cancel booking even in THEIR stadium (403)")
        void managerCannotCancelEvenOwnStadiumBooking() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", bookingP1Id)
                            .header("Authorization", "Bearer " + m1Token))
                    .andExpect(status().isForbidden());
        }

        // ================= BUSINESS POLICIES =================

        @Test @DisplayName("2. 6-Hour Rule: Player fails, Admin bypasses")
        void testSixHourPolicy() throws Exception {
            Stadium stadium = stadiumRepository.findById(stadium1Id).get();
            stadium.setOpenTime(LocalTime.MIDNIGHT);
            stadium.setCloseTime(LocalTime.of(23, 59));
            stadiumRepository.saveAndFlush(stadium);

            LocalDateTime lateTime = LocalDateTime.now().plusHours(2);
            UUID lateId = authUtils.createAndSaveBooking(stadium, player1, lateTime, 1).getId();

            mockMvc.perform(delete(BASE_URL + "/{id}", lateId)
                            .header("Authorization", "Bearer " + p1Token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("6 hours")));

            mockMvc.perform(delete(BASE_URL + "/{id}", lateId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("3. Status Protection: Cannot cancel COMPLETED or already CANCELLED")
        void testStatusProtection() throws Exception {
            var booking = bookingRepository.findById(bookingP1Id).get();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.saveAndFlush(booking);

            mockMvc.perform(delete(BASE_URL + "/{id}", bookingP1Id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("Cannot modify a cancelled or completed booking")));

            booking = bookingRepository.findById(bookingP1Id).get();
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.saveAndFlush(booking);

            mockMvc.perform(delete(BASE_URL + "/{id}", bookingP1Id)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("Cannot modify a cancelled or completed booking")));
        }

        // ================= SOFT DELETE VERIFICATION =================

        @Test
        @DisplayName("4. Soft Delete Verification: Data remains but status becomes CANCELLED")
        void verifySoftDelete() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", bookingP1Id)
                            .header("Authorization", "Bearer " + p1Token))
                    .andExpect(status().isNoContent());

            var deletedBooking = bookingRepository.findById(bookingP1Id);

            assertThat(deletedBooking).isPresent(); // (Not Hard Delete)
            assertThat(deletedBooking.get().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Update Operations (PUT - Modify Booking)")
    class UpdateOperations {

        private final String UPDATE_URL = BASE_URL + "/{id}";

        // ================= SECURITY & RBAC =================

        @Test @DisplayName("1. Anonymous: Should return 401 Unauthorized")
        void update_Anonymous_401() throws Exception {
            var request = new BookingRequestForUpdate(stadium1Id, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(1), "Note");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("2. Manager: Forbidden from updating any booking (403)")
        void update_Manager_Forbidden() throws Exception {
            var request = new BookingRequestForUpdate(null, null, null, "Manager Update");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + m1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test @DisplayName("3. Player BOLA: Cannot update someone else's booking (403)")
        void update_BOLA_403() throws Exception {
            var request = new BookingRequestForUpdate(null, null, null, "BOLA Attack");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + p2Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        // ================= BUSINESS RULES =================

        @Test @DisplayName("4. 6-Hour Rule: Should fail if window is closed (400)")
        void update_LateModification_400() throws Exception {
            Stadium s1 = stadiumRepository.findById(stadium1Id).get();
            LocalDateTime lateStart = LocalDateTime.now().plusHours(2);
            Booking lateBooking = authUtils.createAndSaveBooking(s1, player1, lateStart, 1);

            var request = new BookingRequestForUpdate(null, null, null, "Too late update");

            mockMvc.perform(put(UPDATE_URL, lateBooking.getId())
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("6 hours")));
        }

        @Test @DisplayName("5. Admin Bypass: Admin can update within 6 hours (200)")
        void update_AdminBypass_Success() throws Exception {
            Stadium s1 = stadiumRepository.findById(stadium1Id).get();
            s1.setOpenTime(LocalTime.MIDNIGHT);
            s1.setCloseTime(LocalTime.of(23, 59));
            stadiumRepository.saveAndFlush(s1);

            LocalDateTime nearStart = LocalDateTime.now().plusHours(1);
            Booking lateBooking = authUtils.createAndSaveBooking(s1, player1, nearStart, 1);

            var request = new BookingRequestForUpdate(null, null, null, "Admin fix");

            mockMvc.perform(put(UPDATE_URL, lateBooking.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.note").value("Admin fix"));
        }

        // ================= VALIDATIONS & INTEGRITY =================

        @Test @DisplayName("6. Status Guard: Cannot update COMPLETED (400)")
        void update_InvalidStatus_400() throws Exception {
            var booking = bookingRepository.findById(bookingP1Id).get();
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.saveAndFlush(booking);

            var request = new BookingRequestForUpdate(null, null, null, "Impossible");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("7. Price Recalculation: Stadium change updates price (200)")
        void update_PriceSync_Success() throws Exception {
            Stadium s2 = stadiumRepository.findById(stadium2Id).get();
            s2.setPricePerHour(300.0);
            s2.setBallRentalFee(100);
            stadiumRepository.saveAndFlush(s2);

            LocalDateTime futureDate = LocalDateTime.now().plusDays(10).withHour(18).withMinute(0).withSecond(0).withNano(0);
            var request = new BookingRequestForUpdate(stadium2Id, futureDate, futureDate.plusHours(1), "New Stadium");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPrice").value(400.0));
        }

        @Test @DisplayName("8. Note Only: Should update only note and return 200")
        void update_NoteOnly_Success() throws Exception {
            var request = new BookingRequestForUpdate(null, null, null, "Updated Note Only");
            Double totalPrice = bookingRepository.findById(bookingP1Id).get().getTotalPrice();

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.note").value("Updated Note Only"))
                    .andExpect(jsonPath("$.totalPrice").value(totalPrice));
        }


        @Test @DisplayName("9. Partial Update Duration: Should fail if only startTime is changed but duration exceeds 3h")
        void update_PartialDuration_400() throws Exception {
            LocalDateTime oldStart = LocalDateTime.now().plusDays(2).withHour(17).withMinute(0).withSecond(0).withNano(0);
            Booking original = authUtils.createAndSaveBooking(stadiumRepository.findById(stadium1Id).get(), player1, oldStart, 1);

            LocalDateTime newStart = oldStart.withHour(14); // 14 : 18 -> invalid
            var request = new BookingRequestForUpdate(null, newStart, null, "Sneaky duration update");

            mockMvc.perform(put(UPDATE_URL, original.getId())
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("Stadium is closed during the selected time.")));
        }

        @Test @DisplayName("10. Soft Deleted Stadium: Should return 404 if the stadium is deleted")
        void update_DeletedStadium_404() throws Exception {
            Stadium s1 = stadiumRepository.findById(stadium1Id).get();
            s1.setDeleted(true);
            stadiumRepository.saveAndFlush(s1);

            var request = new BookingRequestForUpdate(null, null, null, "Updating deleted stadium booking");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("Stadium not found")));
        }

        // ================= CONFLICTS =================

        @Test @DisplayName("11. Time Conflict: Cannot update to a slot taken by another player (409)")
        void update_Conflict_409() throws Exception {
            Stadium s1 = stadiumRepository.findById(stadium1Id).get();
            LocalDateTime tomorrow18 = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0);
            authUtils.createAndSaveBooking(s1, player2, tomorrow18, 1);

            var request = new BookingRequestForUpdate(stadium1Id, tomorrow18, tomorrow18.plusHours(1), "Conflict");

            mockMvc.perform(put(UPDATE_URL, bookingP1Id)
                            .header("Authorization", "Bearer " + p1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }
}
