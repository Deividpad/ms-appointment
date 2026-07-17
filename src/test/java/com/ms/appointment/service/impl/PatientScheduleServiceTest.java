package com.ms.appointment.service.impl;

import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.util.PatientStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientScheduleServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientScheduleService patientScheduleService;

    @Captor
    private ArgumentCaptor<LocalDate> dateCaptor;

    @Test
    @DisplayName("unlockPatients unlocks expired patients using today's date")
    void unlockPatients_success() {
        when(patientRepository.unlockExpiredPatients(
                eq(PatientStatus.PENALTY_BLOCKED),
                eq(PatientStatus.AVAILABLE),
                any(LocalDate.class)))
                .thenReturn(3);

        assertDoesNotThrow(() -> patientScheduleService.unlockPatients());

        verify(patientRepository).unlockExpiredPatients(
                eq(PatientStatus.PENALTY_BLOCKED),
                eq(PatientStatus.AVAILABLE),
                dateCaptor.capture());
        assertEquals(LocalDate.now(), dateCaptor.getValue());
        verifyNoMoreInteractions(patientRepository);
    }

    @Test
    @DisplayName("unlockPatients still calls the repository when no patients are unlocked")
    void unlockPatients_noRowsUpdated() {
        when(patientRepository.unlockExpiredPatients(
                PatientStatus.PENALTY_BLOCKED,
                PatientStatus.AVAILABLE,
                LocalDate.of(2026, 7, 17)))
                .thenReturn(0);

        assertDoesNotThrow(() -> patientScheduleService.unlockPatients());

        verify(patientRepository).unlockExpiredPatients(
                PatientStatus.PENALTY_BLOCKED,
                PatientStatus.AVAILABLE,
                LocalDate.of(2026, 7, 17));
        verifyNoMoreInteractions(patientRepository);
    }

    @Test
    @DisplayName("unlockPatients propagates repository failures")
    void unlockPatients_repositoryFailure() {
        doThrow(new IllegalStateException("database down"))
                .when(patientRepository)
                .unlockExpiredPatients(
                        PatientStatus.PENALTY_BLOCKED,
                        PatientStatus.AVAILABLE,
                        LocalDate.of(2026, 7, 17));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> patientScheduleService.unlockPatients());

        assertEquals("database down", ex.getMessage());
        verify(patientRepository).unlockExpiredPatients(
                PatientStatus.PENALTY_BLOCKED,
                PatientStatus.AVAILABLE,
                LocalDate.of(2026, 7, 17));
    }
}
