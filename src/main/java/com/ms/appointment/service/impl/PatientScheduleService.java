package com.ms.appointment.service.impl;

import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.util.PatientStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@EnableScheduling
public class PatientScheduleService {

    private static final Logger log = LoggerFactory.getLogger(PatientScheduleService.class);

    private final PatientRepository patientRepository;

    public PatientScheduleService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    @Scheduled(cron = "${scheduler.patient-unlock.cron}", zone = "America/Bogota")
    public void     unlockPatients() {
        LocalDate now = LocalDate.now();
        int updated = patientRepository.unlockExpiredPatients(PatientStatus.PENALTY_BLOCKED, PatientStatus.AVAILABLE, now);
        if (updated > 0) {
            log.info("{} patients have been automatically unlocked.", updated);
        }
    }

}