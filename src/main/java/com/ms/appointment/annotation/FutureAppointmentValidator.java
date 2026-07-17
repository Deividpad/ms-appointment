package com.ms.appointment.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class FutureAppointmentValidator implements ConstraintValidator<FutureAppointment, LocalDateTime> {

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {

        if (value == null) {
            return true; // @NotNull handles null
        }

        LocalDateTime now = LocalDateTime.now();
        return !value.isBefore(now);
    }
}