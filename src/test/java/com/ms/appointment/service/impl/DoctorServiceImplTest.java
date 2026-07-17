package com.ms.appointment.service.impl;

import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;
import com.ms.appointment.entity.Doctor;
import com.ms.appointment.entity.Specialty;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.repository.DoctorRepository;
import com.ms.appointment.repository.SpecialtyRepository;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    @Captor
    private ArgumentCaptor<Doctor> doctorCaptor;

    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
        specialty.setId(7L);
        specialty.setName("Cardiology");
    }

    @Test
    @DisplayName("registerDoctor saves the doctor when phone, email, and specialty are valid")
    void registerDoctor_success() {
        DoctorRequestDTO request = new DoctorRequestDTO(
                "Dr. Alice Smith",
                7L,
                "3001234567",
                "alice@example.com");

        when(doctorRepository.existsByPhone("3001234567")).thenReturn(false);
        when(doctorRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(specialtyRepository.findById(7L)).thenReturn(Optional.of(specialty));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        DoctorResponseDTO response = doctorService.registerDoctor(request);

        assertEquals(11L, response.id());
        assertEquals("Dr. Alice Smith", response.fullName());
        assertEquals("Cardiology", response.specialty());
        assertEquals("3001234567", response.phone());
        assertEquals("alice@example.com", response.email());

        verify(doctorRepository).existsByPhone("3001234567");
        verify(doctorRepository).existsByEmail("alice@example.com");
        verify(specialtyRepository).findById(7L);
        verify(doctorRepository).save(doctorCaptor.capture());

        Doctor savedDoctor = doctorCaptor.getValue();
        assertEquals("Dr. Alice Smith", savedDoctor.getFullName());
        assertNotNull(savedDoctor.getSpecialty());
        assertEquals(7L, savedDoctor.getSpecialty().getId());
        assertEquals("Cardiology", savedDoctor.getSpecialty().getName());
    }

    @Test
    @DisplayName("registerDoctor rejects duplicate phone before checking email or specialty")
    void registerDoctor_duplicatePhone() {
        DoctorRequestDTO request = new DoctorRequestDTO(
                "Dr. Alice Smith",
                7L,
                "3001234567",
                "alice@example.com");

        when(doctorRepository.existsByPhone("3001234567")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> doctorService.registerDoctor(request));

        assertEquals(ResponseCode.PHONE_ALREADY_EXISTS.getCode(), ex.getMessage());
        verify(doctorRepository).existsByPhone("3001234567");
        verify(doctorRepository, never()).existsByEmail(any());
        verifyNoInteractions(specialtyRepository);
    }

    @Test
    @DisplayName("registerDoctor rejects duplicate email after phone check passes")
    void registerDoctor_duplicateEmail() {
        DoctorRequestDTO request = new DoctorRequestDTO(
                "Dr. Alice Smith",
                7L,
                "3001234567",
                "alice@example.com");

        when(doctorRepository.existsByPhone("3001234567")).thenReturn(false);
        when(doctorRepository.existsByEmail("alice@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> doctorService.registerDoctor(request));

        assertEquals(ResponseCode.EMAIL_ALREADY_EXISTS.getCode(), ex.getMessage());
        verify(doctorRepository).existsByPhone("3001234567");
        verify(doctorRepository).existsByEmail("alice@example.com");
        verifyNoInteractions(specialtyRepository);
    }

    @Test
    @DisplayName("registerDoctor rejects an unknown specialty")
    void registerDoctor_specialtyNotFound() {
        DoctorRequestDTO request = new DoctorRequestDTO(
                "Dr. Alice Smith",
                7L,
                "3001234567",
                "alice@example.com");

        when(doctorRepository.existsByPhone("3001234567")).thenReturn(false);
        when(doctorRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(specialtyRepository.findById(7L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> doctorService.registerDoctor(request));

        assertEquals(ResponseCode.SPECIALTY_NOT_FOUND.getCode(), ex.getMessage());
        verify(doctorRepository).existsByPhone("3001234567");
        verify(doctorRepository).existsByEmail("alice@example.com");
        verify(specialtyRepository).findById(7L);
        verify(doctorRepository, never()).save(any());
    }

    @Test
    @DisplayName("getDoctorById returns the mapped doctor when found")
    void getDoctorById_success() {
        Doctor doctor = new Doctor();
        doctor.setId(11L);
        doctor.setFullName("Dr. Alice Smith");
        doctor.setPhone("3001234567");
        doctor.setEmail("alice@example.com");
        doctor.setSpecialty(specialty);

        when(doctorRepository.findById(11L)).thenReturn(Optional.of(doctor));

        DoctorResponseDTO response = doctorService.getDoctorById(11L);

        assertEquals(11L, response.id());
        assertEquals("Dr. Alice Smith", response.fullName());
        assertEquals("Cardiology", response.specialty());
        assertEquals("3001234567", response.phone());
        assertEquals("alice@example.com", response.email());
        verify(doctorRepository).findById(11L);
    }

    @Test
    @DisplayName("getDoctorById throws when the doctor does not exist")
    void getDoctorById_notFound() {
        when(doctorRepository.findById(11L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> doctorService.getDoctorById(11L));

        assertEquals(ResponseCode.DOCTOR_NOT_FOUND.getCode(), ex.getMessage());
        verify(doctorRepository).findById(11L);
    }
}
