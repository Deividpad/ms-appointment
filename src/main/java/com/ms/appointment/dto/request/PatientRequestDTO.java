package com.ms.appointment.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PatientRequestDTO(
        @NotBlank(message = "Full name is mandatory")
        @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
        String fullName,

        @NotBlank(message = "Identity document is mandatory")
        @Size(min = 7, message = "Identity document must be at least 7 characters")
        String identityDocument,

        @NotBlank(message = "Phone is mandatory")
        @Size(min = 7, message = "Phone must have at least 7 digits")
        String phone,

        @NotBlank(message = "Email is mandatory")
        @Email(message = "Email must be a valid email format")
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
        String email,

        @NotNull(message = "Birth date is mandatory")
        @Past(message = "Birth date must be before today")
        LocalDate birthDate // Used for minimum age 0 business rule
) {}