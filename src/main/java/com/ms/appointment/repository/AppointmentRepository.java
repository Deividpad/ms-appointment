package com.ms.appointment.repository;

import com.ms.appointment.entity.Appointment;
import com.ms.appointment.util.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    Optional<Appointment> findByIdAndStatus(Long id, AppointmentStatus status);
    boolean existsByDoctorIdAndAppointmentDateTimeAndStatus(Long doctorId, LocalDateTime dateTime, AppointmentStatus status);

    boolean existsByPatientIdAndAppointmentDateTimeAndStatus(Long patientId, LocalDateTime dateTime, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(Long doctorId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);

//    @Query("SELECT a FROM Appointment a WHERE " +
//            "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
//            "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
//            "(:status IS NULL OR a.status = :status) AND " +
//            "(:start IS NULL OR a.appointmentDateTime >= :start) AND " +
//            "(:end IS NULL OR a.appointmentDateTime <= :end)")
//    List<Appointment> findAppointmentsWithFilters(
//            @Param("doctorId") Long doctorId,
//            @Param("patientId") Long patientId,
//            @Param("status") AppointmentStatus status,
//            @Param("start") LocalDateTime start,
//            @Param("end") LocalDateTime end);
}