package com.hamza.stadiumbooking.booking.validation;

import com.hamza.stadiumbooking.booking.BookingRequest;
import com.hamza.stadiumbooking.stadium.StadiumResponse;
import com.hamza.stadiumbooking.stadium.StadiumService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class StadiumOpeningValidator implements ConstraintValidator<CheckStadiumOpening, BookingRequest> {

    private final StadiumService stadiumService;

    @Override
    public boolean isValid(BookingRequest request, ConstraintValidatorContext constraintValidatorContext) {
        if (request == null || request.stadiumId() == null || request.startTime() == null || request.endTime() == null) {
            return true;
        }

        try {
            StadiumResponse stadium = stadiumService.getStadiumById(request.stadiumId());
            LocalTime openTime = stadium.openTime();
            LocalTime closeTime = stadium.closeTime();

            LocalTime start = request.startTime().toLocalTime();
            LocalTime end = request.endTime().toLocalTime();

            return TimeValidationUtils.isWithinOperatingHours(start, end, openTime, closeTime);
        } catch (Exception e){
            return false;
        }
    }
}
