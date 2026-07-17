package com.ms.appointment.service;

import com.ms.appointment.entity.Appointment;

import java.time.LocalDateTime;

public interface PenaltyService {
    void validatePenalty(Appointment appointment, LocalDateTime now);
}
