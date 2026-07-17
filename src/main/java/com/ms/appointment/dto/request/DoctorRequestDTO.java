package com.ms.appointment.dto.request;

import jakarta.validation.constraints.*;

public record DoctorRequestDTO(
        @NotBlank(message = "Full name is mandatory")
        @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
        String fullName,

        @NotNull(message = "Specialty Id is mandatory")
        @Positive(message = "Specialty Id must be greater than zero")
        Long specialtyId,

        @Size(min = 7, message = "Phone must have at least 7 digits")
        String phone,

        @Email(message = "Email must be a valid email format")
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
        @Size(min = 7, message = "Phone number must have at least 7 digits")
        String email
) {}
