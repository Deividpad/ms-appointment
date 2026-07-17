package com.ms.appointment.dto.response;

import java.time.LocalDateTime;

public record PenaltyResponseDTO(
        Long id,
        Long patientId,
        Long appointmentId,
        LocalDateTime penaltyDateTime
) {}