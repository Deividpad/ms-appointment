package com.ms.appointment.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureAppointmentValidator.class)
public @interface FutureAppointment {
    String message() default "Appointment date must be today or later";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}