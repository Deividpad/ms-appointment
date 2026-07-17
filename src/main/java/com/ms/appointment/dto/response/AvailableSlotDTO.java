package com.ms.appointment.dto.response;

import java.time.LocalTime;

public record AvailableSlotDTO(
        Integer slot,
        LocalTime start,
        LocalTime end
) {
}