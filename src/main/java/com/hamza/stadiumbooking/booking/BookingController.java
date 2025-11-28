package com.hamza.stadiumbooking.booking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/bookings")
public class BookingController {
    private final BookingService bookingService ;

    @Autowired
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> getAllBookings(
            @RequestParam(required = false) Long stadiumId,
            @RequestParam(required = false) Long userId
    ){
        return bookingService.getAllBookings(stadiumId,userId);
    }

    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable Long id){
        return bookingService.getBookingById(id);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> addBooking(
            @RequestBody BookingRequest bookingRequest
    ){
        BookingResponse bookingResponse = bookingService.addBooking(bookingRequest);
        return new ResponseEntity<>(bookingResponse,HttpStatus.CREATED);
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long bookingId){
        bookingService.deleteBooking(bookingId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long bookingId,
            @RequestBody BookingRequest bookingRequest
    ) {
        BookingResponse bookingResponse = bookingService.updateBooking(
                bookingId,
                bookingRequest.startTime(),
                bookingRequest.numberOfHours(),
                bookingRequest.stadiumId(),
                bookingRequest.userId()
        );
        return new ResponseEntity<>(bookingResponse, HttpStatus.OK);
    }


}
