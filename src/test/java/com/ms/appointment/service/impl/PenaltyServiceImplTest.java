package com.ms.appointment.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.ms.appointment.entity.Appointment;
import com.ms.appointment.entity.Patient;
import com.ms.appointment.entity.Penalty;
import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.repository.PenaltyRepository;
import com.ms.appointment.util.PatientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PenaltyServiceImplTest {

    @Mock
    private PenaltyRepository penaltyRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PenaltyServiceImpl penaltyService;

    private Patient patient;
    private Appointment appointment;
    private LocalDateTime appointmentTime;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setFullName("Test Patient");
        patient.setStatus(PatientStatus.AVAILABLE);

        appointmentTime = LocalDateTime.of(2026, 6, 15, 10, 0, 0); // 10:00 AM
        appointment = new Appointment();
        appointment.setId(100L);
        appointment.setPatient(patient);
        appointment.setAppointmentDateTime(appointmentTime);
    }

    @Test
    @DisplayName("Should NOT save penalty when cancellation is exactly 2 hours before the appointment (Edge Case)")
    void shouldNotSavePenaltyWhenExactlyTwoHoursBefore() {
        // 2 hours before 10:00 AM is 08:00 AM
        LocalDateTime cancelTime = appointmentTime.minusHours(2);

        when(penaltyRepository.countPenaltiesInLast30Days(eq(patient.getId()), any()))
                .thenReturn(0L);

        penaltyService.validatePenalty(appointment, cancelTime);

        verify(penaltyRepository, never()).save(any(Penalty.class));
        assertEquals(PatientStatus.AVAILABLE, patient.getStatus());
    }

    @Test
    @DisplayName("Should save penalty when cancellation is 1 minute late (1h 59m before appointment - Edge Case)")
    void shouldSavePenaltyWhenOneMinuteLate() {
        // 1 hour and 59 minutes before 10:00 AM
        LocalDateTime cancelTime = appointmentTime.minusHours(2).plusMinutes(1);

        when(penaltyRepository.countPenaltiesInLast30Days(eq(patient.getId()), any()))
                .thenReturn(0L);

        penaltyService.validatePenalty(appointment, cancelTime);

        ArgumentCaptor<Penalty> penaltyCaptor = ArgumentCaptor.forClass(Penalty.class);
        verify(penaltyRepository, times(1)).save(penaltyCaptor.capture());

        Penalty savedPenalty = penaltyCaptor.getValue();
        assertEquals(patient, savedPenalty.getPatient());
        assertEquals(appointment, savedPenalty.getAppointment());
        assertEquals(cancelTime, savedPenalty.getPenaltyDateTime());
    }

    @Test
    @DisplayName("Should NOT block patient when active penalties are below the maximum allowed limit")
    void shouldNotBlockPatientWhenPenaltiesAreBelowLimit() {
        LocalDateTime cancelTime = appointmentTime.minusHours(3); // On time cancellation

        // Assuming limit is 3, 2 active penalties should not block him
        when(penaltyRepository.countPenaltiesInLast30Days(eq(patient.getId()), any()))
                .thenReturn(2L);

        penaltyService.validatePenalty(appointment, cancelTime);

        verify(patientRepository, never()).save(any(Patient.class));
        assertEquals(PatientStatus.AVAILABLE, patient.getStatus());
    }

    @Test
    @DisplayName("Should block patient and set unlock date when active penalties reach the limit (Edge Case)")
    void shouldBlockPatientWhenPenaltiesReachLimit() {
        LocalDateTime cancelTime = appointmentTime.minusHours(3); // On time cancellation

        // Simulate that patient has hit the maximum limit (e.g., 3 penalties)
        when(penaltyRepository.countPenaltiesInLast30Days(eq(patient.getId()), any()))
                .thenReturn(3L);

        penaltyService.validatePenalty(appointment, cancelTime);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository, times(1)).save(patientCaptor.capture());

        Patient blockedPatient = patientCaptor.getValue();
        assertEquals(PatientStatus.PENALTY_BLOCKED, blockedPatient.getStatus());
        assertEquals(LocalDate.now().plusDays(30), blockedPatient.getUnlockPenaltyDate());
    }

    @Test
    @DisplayName("Should apply both: save new penalty and block patient simultaneously when criteria match")
    void shouldApplyPenaltyAndBlockSimultaneously() {
        // Late cancellation (1 hour before) -> triggers new penalty
        LocalDateTime cancelTime = appointmentTime.minusHours(1);

        // Before saving the new penalty, DB already tracks 3 historical penalties
        when(penaltyRepository.countPenaltiesInLast30Days(eq(patient.getId()), any()))
                .thenReturn(3L);

        penaltyService.validatePenalty(appointment, cancelTime);

        // Verify penalty creation
        verify(penaltyRepository, times(1)).save(any(Penalty.class));
        // Verify patient profile lockdown
        verify(patientRepository, times(1)).save(patient);
        assertEquals(PatientStatus.PENALTY_BLOCKED, patient.getStatus());
        assertNotNull(patient.getUnlockPenaltyDate());
    }
}