package com.ms.appointment.dto.response;

import java.time.LocalDate;

public record PatientResponseDTO(
        Long id,
        String fullName,
        String identityDocument,
        String phone,
        String email,
        LocalDate birthDate
) {}