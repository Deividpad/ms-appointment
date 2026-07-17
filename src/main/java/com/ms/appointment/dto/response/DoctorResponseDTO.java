package com.ms.appointment.dto.response;

public record DoctorResponseDTO(
        Long id,
        String fullName,
        String specialty,
        String phone,
        String email
) {}