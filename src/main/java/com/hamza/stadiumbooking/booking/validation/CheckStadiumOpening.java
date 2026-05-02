package com.hamza.stadiumbooking.booking.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StadiumOpeningValidator.class)
public @interface CheckStadiumOpening {
    String message() default "Stadium is closed during the selected time. Please check operating hours.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
