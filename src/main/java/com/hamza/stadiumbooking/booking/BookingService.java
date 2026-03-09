package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.exception.ConflictingBookingsException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.security.utils.OwnershipValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


@Service @Slf4j @RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StadiumRepository stadiumRepository;
    private final OwnershipValidationService ownershipValidationService;

    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        UUID currentUserId = ownershipValidationService.getCurrentUserId();
        log.info("Action: getMyBookings | Requesting bookings for User ID: {}", currentUserId);
        Page<Booking> bookings = bookingRepository.findAllByUserId(pageable, currentUserId);
        log.info("Action: getMyBookings | Found {} bookings", bookings.getTotalElements());
        return bookings.map(this::mapToDto);
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable, UUID stadiumId, UUID userId) {
        boolean isAdmin = ownershipValidationService.isAdmin();
        log.info("Action: getAllBookings | Params: stadiumId={}, userId={}, isAdmin={}", stadiumId, userId, isAdmin);

        Page<Booking> bookings;
        if (isAdmin) {
            if (stadiumId != null && userId != null)
                bookings = bookingRepository.findByUserIdAndStadiumId(pageable, userId, stadiumId);

            else if (userId != null)
                bookings = bookingRepository.findByUserId(pageable, userId);

            else if (stadiumId != null)
                bookings = bookingRepository.findByStadiumId(pageable, stadiumId);

            else bookings = bookingRepository.findAll(pageable);

        } else {
            if (stadiumId == null) {
                log.error("Action: getAllBookings | Error: Missing stadiumId for non-admin user");
                throw new ResourceNotFoundException("Error: Stadium ID is required for managers.");
            }

            ownershipValidationService.checkOwnership(stadiumId);

            bookings = (userId != null) ? bookingRepository.findByUserIdAndStadiumId(pageable, userId, stadiumId)
                    : bookingRepository.findByStadiumId(pageable, stadiumId);
        }
        return bookings.map(this::mapToDto);
    }

    public BookingResponse getBookingById(UUID id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(
                () -> {
                    log.error("Action: getBookingById | Booking NOT FOUND with ID: {}", id);
                    return new ResourceNotFoundException("Booking not found with ID: " + id);
                }
        );

        if (!ownershipValidationService.isAdmin()) {

            if (ownershipValidationService.isPlayer())
                ownershipValidationService.checkBookingOwnership(booking.getUser().getId());

            else if (!ownershipValidationService.isStadiumOwner(booking.getStadium().getId()))
                throw new AccessDeniedException("You can only view bookings for your own stadiums.");
        }
        return mapToDto(booking);
    }

    @Transactional
    public BookingResponse addBooking(BookingRequest bookingRequest) {
        if (!bookingRequest.endTime().isAfter(bookingRequest.startTime()))
            throw new IllegalArgumentException("End time must be after start time");

        Stadium stadium = stadiumRepository.findByIdWithLock(bookingRequest.stadiumId()).orElseThrow(() -> new ResourceNotFoundException("Stadium not found."));

        if (!stadium.isOpenAt(bookingRequest.startTime().toLocalTime(), bookingRequest.endTime().toLocalTime()))
            throw new IllegalArgumentException("Stadium is closed during the selected time. Operating hours: " + stadium.getOpenTime() + " to " + stadium.getCloseTime());


        boolean hasConflict = bookingRepository.findConflictingBookingsForNew(bookingRequest.stadiumId(), bookingRequest.startTime(), bookingRequest.endTime());

        if (hasConflict) throw new ConflictingBookingsException("This time is already booked");

        // --- Prevent Race Condition ---
        stadium.setLastLockAt(LocalDateTime.now());
        stadiumRepository.save(stadium);

        User user = ownershipValidationService.getCurrentUser();
        Booking booking = mapToEntity(bookingRequest, user, stadium);
        booking.validateDuration();
        booking.calculateTotalPrice();

        // Save triggers JPA Hooks (@PrePersist) for Duration & Price
        return mapToDto(bookingRepository.save(booking));
    }

    @Transactional
    public void deleteBooking(UUID bookingId) {
        Booking booking = getValidatedBookingForModification(bookingId);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Transactional
    public BookingResponse updateBooking(UUID bookingId, BookingRequestForUpdate request) {
        Booking booking = getValidatedBookingForModification(bookingId);

        UUID targetStadiumId = (request.stadiumId() != null) ? request.stadiumId() : booking.getStadium().getId();

        Stadium targetStadium = stadiumRepository.findByIdWithLock(targetStadiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + targetStadiumId));

        LocalDateTime newStartTime = (request.startTime() != null) ? request.startTime() : booking.getStartTime();
        LocalDateTime newEndTime = (request.endTime() != null) ? request.endTime() : booking.getEndTime();

        if (!newEndTime.isAfter(newStartTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (!targetStadium.isOpenAt(newStartTime.toLocalTime(), newEndTime.toLocalTime())) {
            throw new IllegalArgumentException("Stadium is closed during the selected time.");
        }

        boolean hasConflict = bookingRepository.findConflictingBookingsForUpdate(bookingId,
                targetStadium.getId(), newStartTime, newEndTime);

        if (hasConflict) throw new ConflictingBookingsException("This time is booked");

        targetStadium.setLastLockAt(LocalDateTime.now());
        stadiumRepository.save(targetStadium);

        if (request.note() != null) booking.setNote(request.note());

        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setStadium(targetStadium);
        booking.validateDuration();
        booking.calculateTotalPrice();

        Booking savedBooking = bookingRepository.save(booking);
        return mapToDto(savedBooking);
    }

    // --- PRIVATE HELPERS ---

    /**
     * Centralized logic to fetch and validate if a booking can be modified (Update/Delete).
     */
    private Booking getValidatedBookingForModification(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking not found ID: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) throw new IllegalArgumentException("Cannot modify a cancelled or completed booking.");

        if (!ownershipValidationService.isAdmin()) {
            if (booking.isModificationWindowClosed()) throw new IllegalArgumentException("Cannot modify within 6 hours of start time.");
            ownershipValidationService.checkBookingOwnership(booking.getUser().getId());
        }
        return booking;
    }

    private Booking mapToEntity(BookingRequest request, User user, Stadium stadium) {
        return Booking.builder().user(user).stadium(stadium).startTime(request.startTime()).endTime(request.endTime()).note(request.note()).status(BookingStatus.CONFIRMED).build();
    }

    private BookingResponse mapToDto(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getStartTime(), booking.getEndTime(), booking.getTotalPrice(), booking.getStatus(), booking.getStadium().getId(), booking.getStadium().getName(), booking.getUser().getId(), booking.getUser().getName(), booking.getNote());
    }
}
