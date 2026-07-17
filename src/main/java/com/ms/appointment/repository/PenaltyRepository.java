package com.ms.appointment.repository;

import com.ms.appointment.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    @Query("SELECT COUNT(p) FROM Penalty p WHERE p.patient.id = :patientId AND p.penaltyDateTime >= :since")
    long countPenaltiesInLast30Days(@Param("patientId") Long patientId, @Param("since") LocalDateTime since);
}