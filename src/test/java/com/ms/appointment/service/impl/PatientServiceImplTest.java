package com.ms.appointment.service.impl;

import com.ms.appointment.dto.request.PatientRequestDTO;
import com.ms.appointment.dto.response.PatientResponseDTO;
import com.ms.appointment.entity.Patient;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.util.PatientStatus;
import com.ms.appointment.util.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Captor
    private ArgumentCaptor<Patient> patientCaptor;

    private PatientRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new PatientRequestDTO(
                "John Doe",
                "12345678",
                "3001234567",
                "john@example.com",
                LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("registerPatient saves a new patient with AVAILABLE status")
    void registerPatient_success() {
        when(patientRepository.existsByPhone("3001234567")).thenReturn(false);
        when(patientRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(patientRepository.existsByIdentityDocument("12345678")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        PatientResponseDTO response = patientService.registerPatient(request);

        assertEquals(99L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals("12345678", response.identityDocument());
        assertEquals("3001234567", response.phone());
        assertEquals("john@example.com", response.email());
        assertEquals(LocalDate.of(1990, 1, 1), response.birthDate());

        verify(patientRepository).existsByPhone("3001234567");
        verify(patientRepository).existsByEmail("john@example.com");
        verify(patientRepository).existsByIdentityDocument("12345678");
        verify(patientRepository).save(patientCaptor.capture());
        assertEquals(PatientStatus.AVAILABLE, patientCaptor.getValue().getStatus());
        assertEquals("John Doe", patientCaptor.getValue().getFullName());
    }

    @Test
    @DisplayName("registerPatient rejects duplicate phone before other checks")
    void registerPatient_duplicatePhone() {
        when(patientRepository.existsByPhone("3001234567")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> patientService.registerPatient(request));

        assertEquals(ResponseCode.PHONE_ALREADY_EXISTS.getCode(), ex.getMessage());
        verify(patientRepository).existsByPhone("3001234567");
        verify(patientRepository, never()).existsByEmail(any());
        verify(patientRepository, never()).existsByIdentityDocument(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerPatient rejects duplicate email after phone check passes")
    void registerPatient_duplicateEmail() {
        when(patientRepository.existsByPhone("3001234567")).thenReturn(false);
        when(patientRepository.existsByEmail("john@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> patientService.registerPatient(request));

        assertEquals(ResponseCode.EMAIL_ALREADY_EXISTS.getCode(), ex.getMessage());
        verify(patientRepository).existsByPhone("3001234567");
        verify(patientRepository).existsByEmail("john@example.com");
        verify(patientRepository, never()).existsByIdentityDocument(any());
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerPatient rejects duplicate identity document after phone and email checks pass")
    void registerPatient_duplicateIdentityDocument() {
        when(patientRepository.existsByPhone("3001234567")).thenReturn(false);
        when(patientRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(patientRepository.existsByIdentityDocument("12345678")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> patientService.registerPatient(request));

        assertEquals(ResponseCode.DOCUMENT_ALREADY_EXISTS.getCode(), ex.getMessage());
        verify(patientRepository).existsByPhone("3001234567");
        verify(patientRepository).existsByEmail("john@example.com");
        verify(patientRepository).existsByIdentityDocument("12345678");
        verify(patientRepository, never()).save(any());
    }

    @Test
    @DisplayName("getPatientById returns the mapped patient when found")
    void getPatientById_success() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setFullName("John Doe");
        patient.setIdentityDocument("12345678");
        patient.setPhone("3001234567");
        patient.setEmail("john@example.com");
        patient.setBirthDate(LocalDate.of(1990, 1, 1));

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        PatientResponseDTO response = patientService.getPatientById(1L);

        assertEquals(1L, response.id());
        assertEquals("John Doe", response.fullName());
        assertEquals("12345678", response.identityDocument());
        assertEquals("3001234567", response.phone());
        assertEquals("john@example.com", response.email());
        assertEquals(LocalDate.of(1990, 1, 1), response.birthDate());
        verify(patientRepository).findById(1L);
    }

    @Test
    @DisplayName("getPatientById throws when the patient does not exist")
    void getPatientById_notFound() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> patientService.getPatientById(1L));

        assertEquals(ResponseCode.PATIENT_NOT_FOUND.getCode(), ex.getMessage());
        verify(patientRepository).findById(1L);
    }

    @Test
    @DisplayName("validateBirthDate accepts null birth dates by treating them as today")
    void validateBirthDate_nullBirthDate() {
        Patient patient = new Patient();

        assertDoesNotThrow(() -> patientService.validateBirthDate(patient));
    }

    @Test
    @DisplayName("validateBirthDate accepts past and present birth dates")
    void validateBirthDate_validDates() {
        Patient todayBorn = new Patient();
        todayBorn.setBirthDate(LocalDate.now());

        Patient pastBorn = new Patient();
        pastBorn.setBirthDate(LocalDate.now().minusYears(1));

        assertDoesNotThrow(() -> patientService.validateBirthDate(todayBorn));
        assertDoesNotThrow(() -> patientService.validateBirthDate(pastBorn));
    }

    @Test
    @DisplayName("validateBirthDate rejects future birth dates")
    void validateBirthDate_futureBirthDate() {
        Patient patient = new Patient();
        patient.setBirthDate(LocalDate.now().plusDays(1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientService.validateBirthDate(patient));

        assertEquals(ResponseCode.INVALID_BIRTH_DATE.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("validatePatientStatus accepts available patients")
    void validatePatientStatus_availablePatient() {
        Patient patient = new Patient();
        patient.setStatus(PatientStatus.AVAILABLE);

        assertDoesNotThrow(() -> patientService.validatePatientStatus(patient));
    }

    @Test
    @DisplayName("validatePatientStatus rejects penalty-blocked patients and includes unlock date")
    void validatePatientStatus_blockedPatient() {
        Patient patient = new Patient();
        patient.setStatus(PatientStatus.PENALTY_BLOCKED);
        patient.setUnlockPenaltyDate(LocalDate.now().plusDays(2));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> patientService.validatePatientStatus(patient));

        assertEquals(ResponseCode.PATIENT_PENALTY_BLOCKED.getCode() + " until " + patient.getUnlockPenaltyDate(), ex.getMessage());
    }
}
