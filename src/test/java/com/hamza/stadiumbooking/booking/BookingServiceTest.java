package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.exception.ConflictingBookingsException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.stadium.Type;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import com.hamza.stadiumbooking.security.utils.OwnershipValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookingService bookingService;
    @Mock
    private OwnershipValidationService ownershipValidationService;

    private User manager;
    private final UUID sharedUserId = UUID.randomUUID();
    private User player;
    private final UUID sharedStadiumId = UUID.randomUUID();
    private Stadium sharedStadium;
    private Booking sharedBooking;
    private final LocalDateTime startTime = LocalDateTime.of(2027, 1, 1, 10, 0);
    private final LocalDateTime endTime = LocalDateTime.of(2027, 1, 1, 12, 0);

    private List<Booking> bookingList;
    private Page<Booking> bookingsPage;

    @BeforeEach
    void setUp() {
        UUID sharedAdminId = UUID.randomUUID();
        User admin = new User(
                sharedAdminId, 0L, "Admin Name", "admin@example.com", "0122222222222",
                "pass", LocalDate.of(1980, 1, 1), null, null, Role.ROLE_ADMIN, false
        );

        UUID sharedManagerId = UUID.randomUUID();
        manager = new User(
                sharedManagerId, 0L, "Manager Name", "manager@example.com", "01111111111",
                "pass", LocalDate.of(1990, 1, 1), null, null, Role.ROLE_MANAGER, false
        );

        player = new User(
                sharedUserId, 0L, "Player Name", "player@example.com", "01000000000",
                "pass", LocalDate.of(2000, 5, 20), null, null, Role.ROLE_PLAYER, false
        );

        sharedStadium = new Stadium(
                sharedStadiumId, 0L, "Field Name", "Location", 100.00, "image.com",
                Type.FIVE_A_SIDE, 10, LocalTime.of(8, 0), LocalTime.of(23, 0),
                null, new HashSet<>(), manager, false, null, null
        );

        long hours = 2L;
        Double pricePerHour = sharedStadium.getPricePerHour();
        Integer rentalFee = sharedStadium.getBallRentalFee();
        Double totalPrice = hours * (pricePerHour + rentalFee);

        UUID sharedBookingId = UUID.randomUUID();
        sharedBooking = new Booking(
                sharedBookingId, 0L, startTime, endTime, totalPrice, "Game Note",
                player, sharedStadium, BookingStatus.CONFIRMED, null, null
        );
        bookingList = List.of(sharedBooking);
        bookingsPage = new PageImpl<>(bookingList);
    }

    @Test
    void getMyBookings() {
        Booking sharedBooking2 = new Booking(
                UUID.randomUUID(), 0L, startTime, endTime, 550.00, "Another Note",
                player, sharedStadium, BookingStatus.CONFIRMED, null, null
        );
        bookingList = List.of(sharedBooking, sharedBooking2);
        bookingsPage = new PageImpl<>(bookingList);

        given(ownershipValidationService.getCurrentUserId()).willReturn(sharedUserId);
        given(bookingRepository.findAllByUserId(Pageable.unpaged(), sharedUserId)).willReturn(bookingsPage);

        Page<BookingResponse> response = bookingService.getMyBookings(Pageable.unpaged());

        verify(ownershipValidationService, times(1)).getCurrentUserId();
        verify(bookingRepository, times(1)).findAllByUserId(Pageable.unpaged(), sharedUserId);
        assertThat(response).hasSize(2);
        assertThat(response.getContent()).extracting(BookingResponse::id)
                .containsExactlyInAnyOrder(sharedBooking.getId(), sharedBooking2.getId());

        assertThat(response.getContent()).extracting(BookingResponse::note)
                .contains("Game Note", "Another Note");

        assertThat(response.getContent())
                .allMatch(r -> r.userId().equals(sharedUserId));
    }

    @Test
    void getMyBookings_ShouldReturnEmptyPage_WhenNoBookingsExist() {
        UUID newUserId = UUID.randomUUID();
        given(ownershipValidationService.getCurrentUserId()).willReturn(newUserId);
        given(bookingRepository.findAllByUserId(Pageable.unpaged(), newUserId)).willReturn(Page.empty());

        Page<BookingResponse> response = bookingService.getMyBookings(Pageable.unpaged());

        verify(ownershipValidationService, times(1)).getCurrentUserId();
        verify(bookingRepository, times(1)).findAllByUserId(Pageable.unpaged(), newUserId);
        assertThat(response).isEmpty();
    }

    @Test
    void getAllBookingsForAdminWhenStadiumAndUserIsNotNull() {
        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(bookingRepository.findByUserIdAndStadiumId(Pageable.unpaged(), sharedUserId, sharedStadiumId))
                .willReturn(bookingsPage);

        Page<BookingResponse> response = bookingService.getAllBookings(Pageable.unpaged(), sharedStadiumId, sharedUserId);

        verify(ownershipValidationService, times(1)).isAdmin();
        verify(bookingRepository, times(1)).findByUserIdAndStadiumId(Pageable.unpaged(), sharedUserId, sharedStadiumId);
        assertThat(response.getContent()).extracting(BookingResponse::id).contains(sharedBooking.getId());
    }

    @Test
    void getAllBookingsForAdminWhenStadiumIsNull() {
        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(bookingRepository.findByUserId(Pageable.unpaged(), sharedUserId)).willReturn(bookingsPage);

        Page<BookingResponse> response = bookingService.getAllBookings(Pageable.unpaged(), null, sharedUserId);

        verify(ownershipValidationService, times(1)).isAdmin();
        verify(bookingRepository, times(1)).findByUserId(Pageable.unpaged(), sharedUserId);
        assertThat(response.getContent()).extracting(BookingResponse::userId).contains(sharedUserId);
    }

    @Test
    void getAllBookingsForAdminWhenUserIsNull() {
        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(bookingRepository.findByStadiumId(Pageable.unpaged(), sharedStadiumId)).willReturn(bookingsPage);

        Page<BookingResponse> response = bookingService.getAllBookings(Pageable.unpaged(), sharedStadiumId, null);

        verify(ownershipValidationService, times(1)).isAdmin();
        verify(bookingRepository, times(1)).findByStadiumId(Pageable.unpaged(), sharedStadiumId);
        assertThat(response.getContent()).extracting(BookingResponse::stadiumId).contains(sharedStadiumId);
    }

    @Test
    void getAllBookingsForAdminWhenStadiumAndUserIsNull() {
        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(bookingRepository.findAll(Pageable.unpaged())).willReturn(bookingsPage);

        bookingService.getAllBookings(Pageable.unpaged(), null, null);

        verify(ownershipValidationService, times(1)).isAdmin();
        verify(bookingRepository, times(1)).findAll(Pageable.unpaged());
    }

    @Test
    void getAllBookingsForManagerWhenStadiumAndUserIsNull_ShouldTrowStadiumIsNotFound() {
        given(ownershipValidationService.isAdmin()).willReturn(false);

        assertThatThrownBy(() -> bookingService.getAllBookings(null, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Error: Stadium ID is required for managers.");
    }

    @Test
    void getAllBookingsForManagerWhenStadiumIsNull_ShouldTrowStadiumIsNotFound() {
        given(ownershipValidationService.isAdmin()).willReturn(false);

        assertThatThrownBy(() -> bookingService.getAllBookings(null, null, sharedUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Error: Stadium ID is required for managers.");
    }

    @Test
    void getAllBookingsForManagerWhenUserIsNull() {
        given(ownershipValidationService.isAdmin()).willReturn(false);
        doNothing().when(ownershipValidationService).checkOwnership(sharedStadiumId);
        given(bookingRepository.findByStadiumId(Pageable.unpaged(), sharedStadiumId)).willReturn(bookingsPage);

        bookingService.getAllBookings(Pageable.unpaged(), sharedStadiumId, null);

        verify(ownershipValidationService, times(1)).isAdmin();
        verify(ownershipValidationService, times(1)).checkOwnership(sharedStadiumId);
        verify(bookingRepository, times(1)).findByStadiumId(Pageable.unpaged(), sharedStadiumId);
    }

    @Test
    void getAllBookingsForManager() {
        given(ownershipValidationService.isAdmin()).willReturn(false);
        doNothing().when(ownershipValidationService).checkOwnership(sharedStadiumId);
        given(bookingRepository.findByUserIdAndStadiumId(Pageable.unpaged(), sharedUserId, sharedStadiumId))
                .willReturn(bookingsPage);

        bookingService.getAllBookings(Pageable.unpaged(), sharedStadiumId, sharedUserId);

        verify(ownershipValidationService, times(1)).isAdmin();
        verify(ownershipValidationService, times(1)).checkOwnership(sharedStadiumId);
        verify(bookingRepository, times(1)).findByUserIdAndStadiumId(Pageable.unpaged(), sharedUserId, sharedStadiumId);
    }

    @Test
    void getAllBookingsForManager_ShouldThrowException_WhenOwnershipCheckFails() {
        given(ownershipValidationService.isAdmin()).willReturn(false);
        doThrow(new org.springframework.security.access.AccessDeniedException(
                "Permission denied. You are not the owner of this booking."))
                .when(ownershipValidationService).checkOwnership(sharedStadiumId);

        assertThatThrownBy(() -> bookingService.getAllBookings(Pageable.unpaged(), sharedStadiumId, null))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(bookingRepository, never()).findByStadiumId(any(), any());
    }

    @Test
    void getBookingByIdForAdmin() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(ownershipValidationService.isAdmin()).willReturn(true);

        BookingResponse response = bookingService.getBookingById(sharedBooking.getId());

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        assertThat(response.id()).isEqualTo(sharedBooking.getId());
    }

    @Test
    void getBookingById_ShouldThrowBookingNotFound() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(sharedBooking.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with ID: " + sharedBooking.getId());
    }

    @Test
    void getBookingByIdForPlayer() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        given(ownershipValidationService.isPlayer()).willReturn(true);
        doNothing().when(ownershipValidationService).checkBookingOwnership(sharedBooking.getUser().getId());

        BookingResponse response = bookingService.getBookingById(sharedBooking.getId());

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(ownershipValidationService, times(1)).checkBookingOwnership(sharedBooking.getUser().getId());
        assertThat(response.id()).isEqualTo(sharedBooking.getId());
    }

    @Test
    void getBookingByIdForPlayer_ShouldThrowPlayerNotOwnerThatBooking() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        given(ownershipValidationService.isPlayer()).willReturn(true);
        doThrow(new AccessDeniedException("Permission denied. You are not the owner of this booking."))
                .when(ownershipValidationService).checkBookingOwnership(sharedBooking.getUser().getId());

        assertThatThrownBy(() -> bookingService.getBookingById(sharedBooking.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Permission denied. You are not the owner of this booking.");

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(ownershipValidationService, times(1)).checkBookingOwnership(sharedBooking.getUser().getId());
    }

    @Test
    void getBookingByIdForManager() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        given(ownershipValidationService.isPlayer()).willReturn(false);
        given(ownershipValidationService.isStadiumOwner(sharedStadiumId)).willReturn(true);

        BookingResponse response = bookingService.getBookingById(sharedBooking.getId());

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(ownershipValidationService, times(1)).isStadiumOwner(sharedStadiumId);
        assertThat(response.id()).isEqualTo(sharedBooking.getId());
    }

    @Test
    void getBookingByIdForManager_ShouldThrowManagerNotOwnerThatStadium() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        given(ownershipValidationService.isPlayer()).willReturn(false);
        given(ownershipValidationService.isStadiumOwner(sharedStadiumId)).willReturn(false);

        assertThatThrownBy(() -> bookingService.getBookingById(sharedBooking.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only view bookings for your own stadiums.");

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(ownershipValidationService, times(1)).isStadiumOwner(sharedStadiumId);
    }

    @Test
    void deleteBookingForUser() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        doNothing().when(ownershipValidationService).checkBookingOwnership(sharedBooking.getUser().getId());

        bookingService.deleteBooking(sharedBooking.getId());

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(ownershipValidationService, times(1)).checkBookingOwnership(sharedBooking.getUser().getId());
        verify(bookingRepository, times(1)).save(sharedBooking);
        assertThat(sharedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void deleteBookingById_ShouldThrowBookingNotFound() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.deleteBooking(sharedBooking.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with ID: " + sharedBooking.getId());
    }

    @Test
    void deleteBookingById_ShouldThrowIllegalStateException() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.ofNullable(sharedBooking));
        sharedBooking.setStatus(BookingStatus.CANCELLED);

        assertThatThrownBy(() -> bookingService.deleteBooking(sharedBooking.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete a cancelled or completed booking.");
    }
    @Test
    void deleteBookingById_ShouldThrowIllegalStateException2() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.ofNullable(sharedBooking));
        sharedBooking.setStatus(BookingStatus.COMPLETED);

        assertThatThrownBy(() -> bookingService.deleteBooking(sharedBooking.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete a cancelled or completed booking.");
    }

    @Test
    void deleteBookingForAdmin() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(true);

        bookingService.deleteBooking(sharedBooking.getId());

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(bookingRepository, times(1)).save(sharedBooking);
        assertThat(sharedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
    @Test
    void deleteBooking_shouldThrowIllegalStateException() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        sharedBooking.setStartTime(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> bookingService.deleteBooking(sharedBooking.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You cannot cancel or update the booking within 6 hours of the start time.");
    }

    @Test
    void deleteBooking_shouldThrowValidation() {
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given((ownershipValidationService.isAdmin())).willReturn(false);
        doThrow(new AccessDeniedException("Permission denied. You are not the owner of this booking."))
                .when(ownershipValidationService).checkBookingOwnership(sharedBooking.getUser().getId());

        assertThatThrownBy(() -> bookingService.deleteBooking(sharedBooking.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Permission denied. You are not the owner of this booking.");

        verify(bookingRepository, times(1)).findById(sharedBooking.getId());
        verify(ownershipValidationService, times(1)).checkBookingOwnership(sharedBooking.getUser().getId());
        assertThat(sharedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void addBookingWhenEndIsBeforeStart() {
        BookingRequest invalidRequest = new BookingRequest(
                sharedStadiumId, LocalDateTime.of(2025, 1, 1, 12, 0),
                LocalDateTime.of(2025, 1, 1, 10, 0), "Invalid Time Note");

        assertThatThrownBy(() -> bookingService.addBooking(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End time must be after start time");

        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(stadiumRepository);
        verifyNoInteractions(ownershipValidationService);
    }

    @Test
    void addBookingWhenStadiumIsNotFound() {
        BookingRequest request = new BookingRequest(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "New Booking"
        );
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.addBooking(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stadium not found.");
    }

    @Test
    void addBooking_ShouldThrowException_WhenTimeIsConflict() {
        BookingRequest request = new BookingRequest(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Conflict Note"
        );
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.of(sharedStadium));
        given(bookingRepository.findConflictingBookingsForNew(request.stadiumId(), request.startTime(), request.endTime()))
                .willReturn(true);

        assertThatThrownBy(() -> bookingService.addBooking(request))
                .isInstanceOf(ConflictingBookingsException.class)
                .hasMessageContaining("This time is already booked");
    }

    @Test
    void addBooking_ShouldThrowException_WhenStadiumIsClosed() {
        BookingRequest request = new BookingRequest(
                sharedStadiumId, LocalDateTime.of(2026,2,1,6,0,0),
                LocalDateTime.of(2026,2,1,8,0,0), "Conflict Note"
        );
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.of(sharedStadium));


        assertThatThrownBy(() -> bookingService.addBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stadium is closed during the selected time. Operating hours: " + sharedStadium.getOpenTime() + " to " + sharedStadium.getCloseTime());
    }

    @Test
    void addBooking() {
        BookingRequest request = new BookingRequest(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Success Note"
        );
        given(ownershipValidationService.getCurrentUser()).willReturn(player);
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.of(sharedStadium));
        given(bookingRepository.findConflictingBookingsForNew(request.stadiumId(), request.startTime(), request.endTime()))
                .willReturn(false);

        Booking savedEntity = new Booking(
                UUID.randomUUID(), 0L, request.startTime(), request.endTime(), sharedBooking.getTotalPrice(),
                request.note(), player, sharedStadium, BookingStatus.CONFIRMED, null, null
        );
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedEntity);

        BookingResponse response = bookingService.addBooking(request);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking savedBooking = captor.getValue();

        assertThat(savedBooking.getNote()).isEqualTo("Success Note");
        assertThat(savedBooking.getUser().getId()).isEqualTo(response.userId());
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(savedBooking.getTotalPrice()).isEqualTo(210.0);
    }

    @Test
    void updateBookingWhenBookingIsNotAvailable() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with ID: " + sharedBooking.getId());
    }

    @Test
    void updateBookingWhenBookingIsStatusIsCancelled() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        sharedBooking.setStatus(BookingStatus.CANCELLED);

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update a cancelled or completed booking.");
    }

    @Test
    void updateBookingWhenBookingStatusIsCompleted() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        sharedBooking.setStatus(BookingStatus.COMPLETED);

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update a cancelled or completed booking.");
    }
    @Test
    void updateBookingWhenBookingIsModificationWindowClosed() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2), "Update Note"
        );
        sharedBooking.setStartTime(LocalDateTime.now().plusHours(1));
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You cannot cancel or update the booking within 6 hours of the start time.");
    }

    @Test
    void updateBookingWhenStadiumIsNotAvailable() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(stadiumRepository.findByIdAndIsDeletedFalse(request.stadiumId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stadium not found with ID: " + sharedBooking.getStadium().getId());
    }

    @Test
    void updateBooking_ShouldThrowPermissionDenied_IfNotOwnerAndNotAdmin() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(ownershipValidationService.isAdmin()).willReturn(false);

        doThrow(new AccessDeniedException("Permission denied. You are not the owner of this booking."))
                .when(ownershipValidationService).checkBookingOwnership(sharedBooking.getUser().getId());

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Permission denied. You are not the owner of this booking.");
    }

    @Test
    void updateBooking_ShouldThrowConflictBooking_ForUser() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedBooking.getStadium().getId())).willReturn(Optional.of(sharedStadium));

        given(ownershipValidationService.isAdmin()).willReturn(false);
        doNothing().when(ownershipValidationService).checkBookingOwnership(player.getId());
        given(bookingRepository.findConflictingBookingsForUpdate(sharedBooking.getId(), sharedStadiumId,
                sharedBooking.getStartTime(), sharedBooking.getEndTime())).willReturn(true);

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(ConflictingBookingsException.class)
                .hasMessageContaining("This time is booked");
    }

    @Test
    void updateBooking_ShouldThrowException_WhenStadiumIsClosed() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, LocalDateTime.of(2026,2,1,6,0,0),
                LocalDateTime.of(2026,2,1,8,0,0), "Conflict Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedBooking.getStadium().getId())).willReturn(Optional.of(sharedStadium));

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stadium is closed during the selected time.");
    }

    @Test
    void updateBooking_ShouldThrowConflictBooking_ForAdmin() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Update Note"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedBooking.getStadium().getId())).willReturn(Optional.of(sharedStadium));

        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(bookingRepository.findConflictingBookingsForUpdate(sharedBooking.getId(), sharedStadiumId,
                sharedBooking.getStartTime(), sharedBooking.getEndTime())).willReturn(true);

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), request))
                .isInstanceOf(ConflictingBookingsException.class)
                .hasMessageContaining("This time is booked");
    }

    @Test
    void updateBooking_KeepOldData() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                sharedStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), null
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedBooking.getStadium().getId())).willReturn(Optional.of(sharedStadium));

        given(ownershipValidationService.isAdmin()).willReturn(false);
        doNothing().when(ownershipValidationService).checkBookingOwnership(player.getId());

        given(bookingRepository.findConflictingBookingsForUpdate(sharedBooking.getId(), sharedStadiumId,
                sharedBooking.getStartTime(), sharedBooking.getEndTime())).willReturn(false);

        given(bookingRepository.save(any(Booking.class))).willReturn(sharedBooking);

        BookingResponse response = bookingService.updateBooking(sharedBooking.getId(), request);

        assertThat(response.stadiumId()).isEqualTo(request.stadiumId());
        assertThat(response.endTime()).isEqualTo(request.endTime());
        assertThat(response.note()).isEqualTo("Game Note");
    }

    @Test
    void updateBooking_WhenNewEndTimeIsBeforeNewStartTime() {
        BookingRequestForUpdate invalidRequest = new BookingRequestForUpdate(
                sharedStadiumId, LocalDateTime.of(2025, 1, 1, 12, 0),
                LocalDateTime.of(2025, 1, 1, 10, 0), "Invalid Time");

        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.ofNullable(sharedStadium));
        given(ownershipValidationService.isAdmin()).willReturn(true);

        assertThatThrownBy(() -> bookingService.updateBooking(sharedBooking.getId(), invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void updateBooking_changeOldData_ForUser() {
        UUID newStadiumId = UUID.randomUUID();
        Stadium newStadium = new Stadium(
                newStadiumId, 0L, "Premium Test Field", "New Location", 150.0,
                "new_image.com", Type.SEVEN_A_SIDE, 20, LocalTime.of(8, 0), LocalTime.of(23, 0),
                null, new HashSet<>(), manager, false, null, null
        );
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                newStadiumId, sharedBooking.getStartTime(), sharedBooking.getEndTime(), "Updated Note!!!"
        );
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));
        given(stadiumRepository.findByIdAndIsDeletedFalse(newStadiumId)).willReturn(Optional.of(newStadium));

        given(ownershipValidationService.isAdmin()).willReturn(false);
        doNothing().when(ownershipValidationService).checkBookingOwnership(player.getId());

        given(bookingRepository.findConflictingBookingsForUpdate(sharedBooking.getId(), newStadiumId,
                sharedBooking.getStartTime(), sharedBooking.getEndTime())).willReturn(false);

        given(bookingRepository.save(any(Booking.class))).willReturn(sharedBooking);

        BookingResponse response = bookingService.updateBooking(sharedBooking.getId(), request);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking updatedBooking = captor.getValue();

        assertThat(updatedBooking.getNote()).isEqualTo("Updated Note!!!");
        assertThat(updatedBooking.getStadium().getId()).isEqualTo(newStadiumId);
        assertThat(updatedBooking.getUser()).isEqualTo(player);
        assertThat(updatedBooking.getStartTime()).isEqualTo(request.startTime());
        assertThat(updatedBooking.getEndTime()).isEqualTo(request.endTime());

        assertThat(response.stadiumId()).isEqualTo(newStadiumId);
        assertThat(updatedBooking.getTotalPrice()).isEqualTo(320.0);
    }

    @Test
    void updateBooking_ShouldKeepOldUserAndStadium_WhenRequestIsNull() {
        BookingRequestForUpdate request = new BookingRequestForUpdate(
                null, null, null, null
        );

        given(ownershipValidationService.isAdmin()).willReturn(true);
        given(bookingRepository.findById(sharedBooking.getId())).willReturn(Optional.of(sharedBooking));

        given(bookingRepository.findConflictingBookingsForUpdate(sharedBooking.getId(), sharedStadiumId,
                sharedBooking.getStartTime(), sharedBooking.getEndTime())).willReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(sharedBooking);

        bookingService.updateBooking(sharedBooking.getId(), request);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        Booking updatedBooking = captor.getValue();

        assertThat(updatedBooking.getUser()).isEqualTo(sharedBooking.getUser());
        assertThat(updatedBooking.getStadium()).isEqualTo(sharedBooking.getStadium());

        verify(userRepository, never()).findById(any(UUID.class));
        verify(stadiumRepository, never()).findById(any(UUID.class));
    }
}