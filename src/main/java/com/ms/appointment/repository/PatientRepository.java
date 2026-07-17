package com.ms.appointment.repository;

import com.ms.appointment.entity.Patient;
import com.ms.appointment.util.PatientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByIdentityDocument(String identityDocument);

    @Query("""
                UPDATE Patient p
                   SET p.status = :enabledStatus,
                       p.unlockPenaltyDate = null
                 WHERE p.status = :blockedStatus
                   AND p.unlockPenaltyDate <= :now
            """)
    @Modifying(clearAutomatically = true)
    int unlockExpiredPatients(
            @Param("blockedStatus") PatientStatus blockedStatus,
            @Param("enabledStatus") PatientStatus enabledStatus,
            @Param("now") LocalDate now
    );
}