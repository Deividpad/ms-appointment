package com.ms.appointment.service.impl;

import com.ms.appointment.entity.Appointment;
import com.ms.appointment.entity.Patient;
import com.ms.appointment.entity.Penalty;
import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.repository.PenaltyRepository;
import com.ms.appointment.service.PenaltyService;
import com.ms.appointment.util.ClinicScheduleUtil;
import com.ms.appointment.util.PatientStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final PatientRepository patientRepository;

    public PenaltyServiceImpl(PenaltyRepository penaltyRepository, PatientRepository patientRepository) {
        this.penaltyRepository = penaltyRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional
    public void validatePenalty(Appointment appointment, LocalDateTime now) {

        // RN-05: Late cancellation penalty validation (< 2 hours check)
        if (now.plusHours(2).isAfter(appointment.getAppointmentDateTime())) {
            Penalty penalty = Penalty.builder()
                    .patient(appointment.getPatient())
                    .appointment(appointment)
                    .penaltyDateTime(now)
                    .build();
            penaltyRepository.save(penalty);
        }

        // RN-05: Check Penalities Block (3 or more penalties in last 30 days)
        long activePenalties = penaltyRepository.countPenaltiesInLast30Days(appointment.getPatient().getId(),
                ClinicScheduleUtil.subtractDaysToPenaltyDateValidation(now));
        if (activePenalties >= ClinicScheduleUtil.MAX_PENALTIES_PATIENT) {
            Patient patient = appointment.getPatient();
            patient.setStatus(PatientStatus.PENALTY_BLOCKED);
            patient.setUnlockPenaltyDate(LocalDate.now().plusDays(30));
            patientRepository.save(patient);
        }
    }
}
