package com.ms.appointment.entity;

import com.ms.appointment.util.PatientStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "identity_document", nullable = false, unique = true, length = 30)
    private String identityDocument;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "unlock_penalty_date")
    private LocalDate unlockPenaltyDate;

    @Enumerated(EnumType.STRING)
    private PatientStatus status;
}
