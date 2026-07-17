package com.ms.appointment.repository.impl;

import com.ms.appointment.entity.Appointment;
import com.ms.appointment.util.AppointmentStatus;
import com.ms.appointment.util.QueryColumns;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class AppointmentSpecification {

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> hasDoctor(Long doctorId) {
        return (root, query, cb) ->
                doctorId == null
                        ? null
                        : cb.equal(root.get(QueryColumns.DOCTOR).get(QueryColumns.ID), doctorId);
    }

    public static Specification<Appointment> hasPatient(Long patientId) {
        return (root, query, cb) ->
                patientId == null
                        ? null
                        : cb.equal(root.get(QueryColumns.PATIENT).get(QueryColumns.ID), patientId);
    }

    public static Specification<Appointment> hasStatus(AppointmentStatus status) {
        return (root, query, cb) ->
                status == null
                        ? null
                        : cb.equal(root.get(QueryColumns.STATUS), status);
    }

    public static Specification<Appointment> appointmentFrom(LocalDateTime start) {
        return (root, query, cb) ->
                start == null
                        ? null
                        : cb.greaterThanOrEqualTo(root.get(QueryColumns.APPOINTMENT_DATE_TIME), start);
    }

    public static Specification<Appointment> appointmentUntil(LocalDateTime end) {
        return (root, query, cb) ->
                end == null
                        ? null
                        : cb.lessThanOrEqualTo(root.get(QueryColumns.APPOINTMENT_DATE_TIME), end);
    }
}
