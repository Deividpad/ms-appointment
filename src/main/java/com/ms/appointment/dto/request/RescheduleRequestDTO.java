package com.ms.appointment.dto.request;

import com.ms.appointment.annotation.FutureAppointment;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleRequestDTO(
        @FutureAppointment
        @NotNull(message = "New appointment date and time are mandatory")
        LocalDateTime newAppointmentDateTime
) {}