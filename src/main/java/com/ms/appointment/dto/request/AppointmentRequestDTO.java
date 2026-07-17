package com.ms.appointment.dto.request;

import com.ms.appointment.annotation.FutureAppointment;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AppointmentRequestDTO(
        @NotNull(message = "Patient ID is mandatory")
        Long patientId,

        @NotNull(message = "Doctor ID is mandatory")
        Long doctorId,

        @FutureAppointment
        @NotNull(message = "Appointment date and time are mandatory")
        LocalDateTime appointmentDateTime
) {}