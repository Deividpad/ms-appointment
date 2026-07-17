package com.ms.appointment.dto.response;

import com.ms.appointment.util.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponseDTO(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        LocalDateTime appointmentDateTime,
        AppointmentStatus status
) {}