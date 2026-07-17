package com.ms.appointment.dto.request;

import com.ms.appointment.annotation.FutureAppointment;
import com.ms.appointment.util.AppointmentStatus;
import jakarta.validation.Valid;

import java.time.LocalDate;

public record AppointmentFilterDTO (
        Long doctorId,
        Long patientId,
        AppointmentStatus status,
        LocalDate start,
        LocalDate end,
        @Valid
        PageRequestDTO pageable
) {}