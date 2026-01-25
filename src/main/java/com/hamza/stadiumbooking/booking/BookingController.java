package com.hamza.stadiumbooking.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController @RequiredArgsConstructor @Slf4j
@RequestMapping(path = "/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BookingResponse> getAdminGlobalBookings(@ParameterObject
                                                        @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable){
        log.info("Incoming request to get ALL global bookings (Admin View)");
        return bookingService.getAllBookings(pageable, null, null);
    }

    @GetMapping("/stadiums/{stadiumId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<BookingResponse> getAllBookings(@ParameterObject
                                                @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                                @PathVariable UUID stadiumId){
        log.info("Incoming request to get bookings for Stadium ID: {}", stadiumId);
        return bookingService.getAllBookings(pageable, stadiumId, null);
    }

    @GetMapping("/players/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BookingResponse>> getBookingsByPlayer(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable UUID playerId) {
        log.info("Admin fetching bookings for player: {}", playerId);
        return ResponseEntity.ok(bookingService.getAllBookings(pageable, null, playerId));
    }

    @GetMapping("/stadiums/{stadiumId}/players/{playerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Page<BookingResponse>> getPlayerBookingsInStadium(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @PathVariable UUID stadiumId,
            @PathVariable UUID playerId) {

        log.info("Request to get bookings for Player ID: {} inside Stadium ID: {}", playerId, stadiumId);

        return ResponseEntity.ok(bookingService.getAllBookings(pageable, stadiumId, playerId));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('PLAYER', 'ADMIN')")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(@ParameterObject
                                                               @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Incoming request to get My Bookings (Player View)");
        return ResponseEntity.ok(bookingService.getMyBookings(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PLAYER')")
    public BookingResponse getBookingById(@PathVariable UUID id){
        log.info("Incoming request to get the booking with ID {}", id);
        return bookingService.getBookingById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLAYER', 'ADMIN')")
    public ResponseEntity<BookingResponse> addBooking(@RequestBody @Valid BookingRequest bookingRequest){
        log.info("Incoming request to create booking for Stadium ID: {} at {}", bookingRequest.stadiumId(), bookingRequest.startTime());
        BookingResponse bookingResponse = bookingService.addBooking(bookingRequest);
        return new ResponseEntity<>(bookingResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLAYER')")
    public ResponseEntity<Void> deleteBooking(@PathVariable UUID bookingId){
        log.info("Incoming request to delete booking with ID: {}", bookingId);
        bookingService.deleteBooking(bookingId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLAYER')")
    public ResponseEntity<BookingResponse> updateBooking(@PathVariable UUID bookingId,
                                                         @RequestBody @Valid BookingRequestForUpdate bookingRequestForUpdate
    ) {
        log.info("Incoming request to update booking with ID: {}", bookingId);
        BookingResponse bookingResponse = bookingService.updateBooking(bookingId, bookingRequestForUpdate);
        return new ResponseEntity<>(bookingResponse, HttpStatus.OK);
    }
}