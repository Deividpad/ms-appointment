package com.ms.appointment.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AvailableDayResponseDTO(
        LocalDate date,
        List<AvailableSlotDTO> slots
) {
}