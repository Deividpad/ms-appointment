package com.ms.appointment.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.ms.appointment.entity.Appointment;
import com.ms.appointment.util.AppointmentStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentSpecificationTest {

    @Mock
    private Root<Appointment> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        // Reiniciar comportamientos comunes si es necesario
    }

    // --- TESTS FOR hasDoctor ---

    @Test
    @DisplayName("hasDoctor: Should return null predicate when doctorId is null")
    void hasDoctorShouldReturnNullWhenIdIsNull() {
        Specification<Appointment> spec = AppointmentSpecification.hasDoctor(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    @Test
    @DisplayName("hasDoctor: Should build equal predicate when doctorId is provided")
    void hasDoctorShouldBuildPredicateWhenIdIsProvided() {
        Long doctorId = 1L;
        // Simular root.get(QueryColumns.DOCTOR).get(QueryColumns.ID)
        Path<Object> doctorPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);

        when(root.get(anyString())).thenReturn(doctorPath);
        when(doctorPath.get(anyString())).thenReturn(idPath);
        when(cb.equal(idPath, doctorId)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecification.hasDoctor(doctorId);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, times(1)).equal(idPath, doctorId);
    }

    // --- TESTS FOR hasPatient ---

    @Test
    @DisplayName("hasPatient: Should return null predicate when patientId is null")
    void hasPatientShouldReturnNullWhenIdIsNull() {
        Specification<Appointment> spec = AppointmentSpecification.hasPatient(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    @Test
    @DisplayName("hasPatient: Should build equal predicate when patientId is provided")
    void hasPatientShouldBuildPredicateWhenIdIsProvided() {
        Long patientId = 5L;
        Path<Object> patientPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);

        when(root.get(anyString())).thenReturn(patientPath);
        when(patientPath.get(anyString())).thenReturn(idPath);
        when(cb.equal(idPath, patientId)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecification.hasPatient(patientId);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, times(1)).equal(idPath, patientId);
    }

    // --- TESTS FOR hasStatus ---

    @Test
    @DisplayName("hasStatus: Should return null predicate when status is null")
    void hasStatusShouldReturnNullWhenStatusIsNull() {
        Specification<Appointment> spec = AppointmentSpecification.hasStatus(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    @Test
    @DisplayName("hasStatus: Should build equal predicate when status is provided")
    void hasStatusShouldBuildPredicateWhenStatusIsProvided() {
        AppointmentStatus status = AppointmentStatus.SCHEDULED;

        when(root.get(anyString())).thenReturn(path);
        when(cb.equal(path, status)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecification.hasStatus(status);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, times(1)).equal(path, status);
    }

    // --- TESTS FOR appointmentFrom ---

    @Test
    @DisplayName("appointmentFrom: Should return null predicate when start date is null")
    void appointmentFromShouldReturnNullWhenStartIsNull() {
        Specification<Appointment> spec = AppointmentSpecification.appointmentFrom(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    // --- TESTS FOR appointmentUntil ---

    @Test
    @DisplayName("appointmentUntil: Should return null predicate when end date is null")
    void appointmentUntilShouldReturnNullWhenEndIsNull() {
        Specification<Appointment> spec = AppointmentSpecification.appointmentUntil(null);
        Predicate result = spec.toPredicate(root, query, cb);
        assertNull(result);
    }

    // --- TESTS FOR appointmentFrom ---

    @Test
    @DisplayName("appointmentFrom: Should build greaterThanOrEqualTo predicate when start date is provided")
    void appointmentFromShouldBuildPredicateWhenStartIsProvided() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 15, 8, 0);

        // CORRECCIÓN: Usar Path en lugar de Expression para evitar el ClassCastException
        Path<LocalDateTime> datePath = mock(Path.class);

        when(root.<LocalDateTime>get(anyString())).thenReturn(datePath);
        when(cb.greaterThanOrEqualTo(datePath, start)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecification.appointmentFrom(start);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, times(1)).greaterThanOrEqualTo(datePath, start);
    }

    // --- TESTS FOR appointmentUntil ---

    @Test
    @DisplayName("appointmentUntil: Should build lessThanOrEqualTo predicate when end date is provided")
    void appointmentUntilShouldBuildPredicateWhenEndIsProvided() {
        LocalDateTime end = LocalDateTime.of(2026, 6, 15, 18, 0);

        // CORRECCIÓN: Usar Path en lugar de Expression para evitar el ClassCastException
        Path<LocalDateTime> datePath = mock(Path.class);

        when(root.<LocalDateTime>get(anyString())).thenReturn(datePath);
        when(cb.lessThanOrEqualTo(datePath, end)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecification.appointmentUntil(end);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, times(1)).lessThanOrEqualTo(datePath, end);
    }
}