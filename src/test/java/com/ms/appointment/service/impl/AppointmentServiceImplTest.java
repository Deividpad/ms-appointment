package com.ms.appointment.service.impl;

import com.ms.appointment.dto.request.AppointmentFilterDTO;
import com.ms.appointment.dto.request.AppointmentRequestDTO;
import com.ms.appointment.dto.request.PageRequestDTO;
import com.ms.appointment.dto.request.RescheduleRequestDTO;
import com.ms.appointment.dto.response.AppointmentResponseDTO;
import com.ms.appointment.dto.response.AvailableDayResponseDTO;
import com.ms.appointment.dto.response.AvailableSlotDTO;
import com.ms.appointment.dto.response.PageResponse;
import com.ms.appointment.entity.Appointment;
import com.ms.appointment.entity.Doctor;
import com.ms.appointment.entity.Patient;
import com.ms.appointment.entity.Specialty;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.InvalidSortFieldException;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.repository.AppointmentRepository;
import com.ms.appointment.repository.DoctorRepository;
import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.service.PatientService;
import com.ms.appointment.service.PenaltyService;
import com.ms.appointment.util.AppointmentStatus;
import com.ms.appointment.util.ClinicScheduleUtil;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private PenaltyService penaltyService;

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Captor
    private ArgumentCaptor<LocalDateTime> dateTimeCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private Patient patient;
    private Doctor doctor;
    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
        specialty.setId(9L);
        specialty.setName("Cardiology");

        patient = new Patient();
        patient.setId(1L);
        patient.setFullName("John Doe");
        patient.setStatus(null);

        doctor = new Doctor();
        doctor.setId(2L);
        doctor.setFullName("Dr. Smith");
        doctor.setSpecialty(specialty);
        doctor.setPhone("3001234567");
        doctor.setEmail("drsmith@example.com");
    }

    @Test
    @DisplayName("bookAppointment creates a scheduled appointment and trims seconds for availability checks")
    void bookAppointment_success() {
        LocalDateTime requestedDateTime = LocalDateTime.of(2026, 7, 21, 9, 0, 45);
        AppointmentRequestDTO request = new AppointmentRequestDTO(1L, 2L, requestedDateTime);

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(eq(2L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndAppointmentDateTimeAndStatus(eq(1L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(55L);
            return saved;
        });

        AppointmentResponseDTO response = appointmentService.bookAppointment(request);

        assertEquals(55L, response.id());
        assertEquals(1L, response.patientId());
        assertEquals("John Doe", response.patientName());
        assertEquals(2L, response.doctorId());
        assertEquals("Dr. Smith", response.doctorName());
        assertEquals(requestedDateTime, response.appointmentDateTime());
        assertEquals(AppointmentStatus.SCHEDULED, response.status());

        verify(patientService).validatePatientStatus(patient);
        verify(patientService).validateBirthDate(patient);
        verify(appointmentRepository).existsByDoctorIdAndAppointmentDateTimeAndStatus(
                2L, LocalDateTime.of(2026, 7, 21, 9, 0), AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).existsByPatientIdAndAppointmentDateTimeAndStatus(
                1L, LocalDateTime.of(2026, 7, 21, 9, 0), AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("bookAppointment throws when the patient does not exist")
    void bookAppointment_patientNotFound() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 9, 0))));

        assertEquals(ResponseCode.PATIENT_NOT_FOUND.getCode(), ex.getMessage());
        verifyNoInteractions(patientService, doctorRepository, appointmentRepository);
    }

    @Test
    @DisplayName("bookAppointment propagates patient status validation failures")
    void bookAppointment_patientBlocked() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        doThrow(new BusinessException("Patient blocked")).when(patientService).validatePatientStatus(patient);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 9, 0))));

        assertEquals("Patient blocked", ex.getMessage());
        verify(patientService).validatePatientStatus(patient);
        verifyNoInteractions(doctorRepository, appointmentRepository);
    }

    @Test
    @DisplayName("bookAppointment throws when the doctor does not exist")
    void bookAppointment_doctorNotFound() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 9, 0))));

        assertEquals(ResponseCode.DOCTOR_NOT_FOUND.getCode(), ex.getMessage());
        verify(patientService).validatePatientStatus(patient);
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("bookAppointment rejects minutes that are not aligned to the 30-minute schedule")
    void bookAppointment_invalidMinuteInterval() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 9, 15))));

        assertEquals(ResponseCode.INVALID_APPOINTMENT_TIME.getCode(), ex.getMessage());
        verify(patientService).validateBirthDate(patient);
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("bookAppointment rejects Sundays")
    void bookAppointment_clinicClosedSunday() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 19, 9, 0))));

        assertEquals(ResponseCode.CLINIC_CLOSED_SUNDAY.getCode(), ex.getMessage());
        verify(patientService).validateBirthDate(patient);
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("bookAppointment rejects holidays")
    void bookAppointment_holiday() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 1, 1, 9, 0))));

        assertEquals(ResponseCode.CLINIC_IS_CLOSED_HOLIDAYS.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("bookAppointment rejects Saturday appointments outside the allowed window")
    void bookAppointment_invalidSaturdaySchedule() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 18, 7, 30))));

        assertEquals(ResponseCode.INVALID_SATURDAY_SCHEDULE.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("bookAppointment rejects weekday appointments outside the allowed window")
    void bookAppointment_invalidWeekdaySchedule() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 7, 30))));

        assertEquals(ResponseCode.INVALID_WEEKDAY_SCHEDULE.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("bookAppointment rejects when the doctor already has an appointment at that time")
    void bookAppointment_doctorAlreadyBooked() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(eq(2L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 9, 0))));

        assertEquals(ResponseCode.DOCTOR_ALREADY_BOOKED.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("bookAppointment rejects when the patient already has an appointment at that time")
    void bookAppointment_patientAlreadyBooked() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(eq(2L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndAppointmentDateTimeAndStatus(eq(1L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.bookAppointment(new AppointmentRequestDTO(1L, 2L, LocalDateTime.of(2026, 7, 21, 9, 0))));

        assertEquals(ResponseCode.PATIENT_ALREADY_BOOKED.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("cancelAppointment cancels the appointment and stamps the cancellation time")
    void cancelAppointment_success() {
        Appointment appointment = scheduledAppointment(10L, LocalDateTime.of(2026, 7, 21, 9, 0));

        when(appointmentRepository.findByIdAndStatus(10L, AppointmentStatus.SCHEDULED)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponseDTO response = appointmentService.cancelAppointment(10L);

        assertEquals(AppointmentStatus.CANCELED, response.status());
        assertNotNull(response.appointmentDateTime());
        assertEquals(10L, response.id());
        assertNotNull(appointment.getCanceledAt());
        assertEquals(AppointmentStatus.CANCELED, appointment.getStatus());

        verify(patientService).validatePatientStatus(patient);
        verify(penaltyService).validatePenalty(eq(appointment), dateTimeCaptor.capture());
        verify(appointmentRepository).save(appointment);
        assertNotNull(dateTimeCaptor.getValue());
    }

    @Test
    @DisplayName("cancelAppointment throws when the appointment does not exist")
    void cancelAppointment_notFound() {
        when(appointmentRepository.findByIdAndStatus(10L, AppointmentStatus.SCHEDULED)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.cancelAppointment(10L));

        assertEquals(ResponseCode.APPOINTMENT_NOT_FOUND.getCode(), ex.getMessage());
        verifyNoInteractions(patientService, penaltyService);
    }

    @Test
    @DisplayName("rescheduleAppointment cancels the old appointment and creates a new one")
    void rescheduleAppointment_success() {
        Appointment oldAppointment = scheduledAppointment(10L, LocalDateTime.of(2026, 7, 21, 9, 0));
        LocalDateTime newDateTime = LocalDateTime.of(2026, 7, 21, 10, 30, 59);
        RescheduleRequestDTO request = new RescheduleRequestDTO(newDateTime);

        when(appointmentRepository.findByIdAndStatus(10L, AppointmentStatus.SCHEDULED)).thenReturn(Optional.of(oldAppointment));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(eq(2L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(false);
        when(appointmentRepository.existsByPatientIdAndAppointmentDateTimeAndStatus(eq(1L), any(LocalDateTime.class), eq(AppointmentStatus.SCHEDULED)))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(77L);
            }
            return saved;
        });

        AppointmentResponseDTO response = appointmentService.rescheduleAppointment(10L, request);

        assertEquals(77L, response.id());
        assertEquals(AppointmentStatus.SCHEDULED, response.status());
        assertEquals(newDateTime, response.appointmentDateTime());

        verify(patientService).validatePatientStatus(patient);
        verify(patientService).validateBirthDate(patient);
        verify(penaltyService).validatePenalty(eq(oldAppointment), any(LocalDateTime.class));
        verify(appointmentRepository).existsByDoctorIdAndAppointmentDateTimeAndStatus(
                2L, LocalDateTime.of(2026, 7, 21, 10, 30), AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).existsByPatientIdAndAppointmentDateTimeAndStatus(
                1L, LocalDateTime.of(2026, 7, 21, 10, 30), AppointmentStatus.SCHEDULED);
        assertEquals(AppointmentStatus.CANCELED, oldAppointment.getStatus());
    }

    @Test
    @DisplayName("rescheduleAppointment throws when the appointment does not exist")
    void rescheduleAppointment_notFound() {
        when(appointmentRepository.findByIdAndStatus(10L, AppointmentStatus.SCHEDULED)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.rescheduleAppointment(10L, new RescheduleRequestDTO(LocalDateTime.of(2026, 7, 21, 10, 30))));

        assertEquals(ResponseCode.APPOINTMENT_NOT_FOUND.getCode(), ex.getMessage());
        verifyNoInteractions(patientService, penaltyService);
    }

    @Test
    @DisplayName("rescheduleAppointment rejects invalid new appointment times before saving the old appointment")
    void rescheduleAppointment_invalidNewDate() {
        Appointment oldAppointment = scheduledAppointment(10L, LocalDateTime.of(2026, 7, 21, 9, 0));

        when(appointmentRepository.findByIdAndStatus(10L, AppointmentStatus.SCHEDULED)).thenReturn(Optional.of(oldAppointment));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> appointmentService.rescheduleAppointment(
                        10L,
                        new RescheduleRequestDTO(
                                LocalDateTime.of(2026, 7, 21, 7, 30)))
        );

        assertEquals(ResponseCode.INVALID_WEEKDAY_SCHEDULE.getCode(), ex.getMessage());

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(appointmentRepository, never())
                .existsByDoctorIdAndAppointmentDateTimeAndStatus(any(), any(), any());

        verify(appointmentRepository, never())
                .existsByPatientIdAndAppointmentDateTimeAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("getAvailableSlots returns open slots and skips occupied ones")
    void getAvailableSlots_success() {
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        LocalDate friday = LocalDate.of(2026, 7, 17);
        LocalDate saturday = LocalDate.of(2026, 7, 18);
        Appointment occupiedOne = scheduledAppointment(11L, friday.atTime(9, 0));
        Appointment occupiedTwo = scheduledAppointment(12L, friday.atTime(9, 30));

        when(appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                2L, friday.atStartOfDay(), friday.atTime(LocalTime.MAX), AppointmentStatus.SCHEDULED))
                .thenReturn(List.of(occupiedOne, occupiedTwo));
        when(appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                2L, saturday.atStartOfDay(), saturday.atTime(LocalTime.MAX), AppointmentStatus.SCHEDULED))
                .thenReturn(List.of());

        List<AvailableDayResponseDTO> response = appointmentService.getAvailableSlots(2L, friday, saturday);

        assertEquals(2, response.size());
        assertEquals(friday, response.get(0).date());
        assertEquals(saturday, response.get(1).date());
        assertEquals(18, response.get(0).slots().size());
        assertEquals(10, response.get(1).slots().size());
        assertFalse(response.get(0).slots().stream().map(AvailableSlotDTO::start).anyMatch(time -> time.equals(LocalTime.of(9, 0))));
        assertFalse(response.get(0).slots().stream().map(AvailableSlotDTO::start).anyMatch(time -> time.equals(LocalTime.of(9, 30))));
        assertEquals(LocalTime.of(8, 0), response.get(0).slots().get(0).start());
        assertEquals(LocalTime.of(12, 30), response.get(1).slots().get(9).start());
    }

    @Test
    @DisplayName("getAvailableSlots returns an empty list for Sundays without querying occupied appointments")
    void getAvailableSlots_sunday() {
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        List<AvailableDayResponseDTO> response = appointmentService.getAvailableSlots(
                2L,
                LocalDate.of(2026, 7, 19),
                LocalDate.of(2026, 7, 19));

        assertEquals(1, response.size());
        assertEquals(0, response.get(0).slots().size());
        verify(appointmentRepository, never()).findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAvailableSlots returns an empty list for holidays without querying occupied appointments")
    void getAvailableSlots_holiday() {
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        List<AvailableDayResponseDTO> response = appointmentService.getAvailableSlots(
                2L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 1));

        assertEquals(1, response.size());
        assertEquals(0, response.get(0).slots().size());
        verify(appointmentRepository, never()).findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAvailableSlots throws when the doctor does not exist")
    void getAvailableSlots_doctorNotFound() {
        when(doctorRepository.findById(2L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.getAvailableSlots(2L, LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 18)));

        assertEquals(ResponseCode.DOCTOR_NOT_FOUND.getCode(), ex.getMessage());
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("getAvailableSlots rejects an invalid date range")
    void getAvailableSlots_invalidDateRange() {
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.getAvailableSlots(2L, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 19)));

        assertEquals(ResponseCode.INVALID_DATE_RANGE.getCode(), ex.getMessage());
        verify(appointmentRepository, never()).findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAvailableSlots rejects ranges larger than 30 days")
    void getAvailableSlots_rangeTooLarge() {
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.getAvailableSlots(2L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 5)));

        assertEquals(ResponseCode.DATE_RANGE_TOO_LARGE.getCode(), ex.getMessage());
    }

    @Test
    @DisplayName("getAppointments returns a mapped page with default sorting")
    void getAppointments_success() {
        Appointment appointment = scheduledAppointment(30L, LocalDateTime.of(2026, 7, 21, 9, 0));
        AppointmentFilterDTO dto = new AppointmentFilterDTO(
                2L,
                null,
                null,
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 21),
                PageRequestDTO.builder()
                        .page(1)
                        .size(10)
                        .sortBy(null)
                        .sortDirection("ASC")
                        .build());

        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findAll(any(Specification.class), pageableCaptor.capture()))
                .thenReturn(new PageImpl<>(List.of(appointment), PageRequest.of(0, 10, Sort.by("appointmentDateTime")), 1));

        PageResponse<AppointmentResponseDTO> response = appointmentService.getAppointments(dto);

        assertEquals(1, response.getData().size());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1L, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isLast());
        assertEquals(appointment.getId(), response.getData().get(0).id());
        assertEquals(appointment.getPatient().getFullName(), response.getData().get(0).patientName());
        assertEquals(appointment.getDoctor().getFullName(), response.getData().get(0).doctorName());

        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(10, pageableCaptor.getValue().getPageSize());
        assertEquals("appointmentDateTime", pageableCaptor.getValue().getSort().getOrderFor("appointmentDateTime").getProperty());
        verify(doctorRepository).findById(2L);
    }

    @Test
    @DisplayName("getAppointments rejects invalid sort fields before touching the repository")
    void getAppointments_invalidSortField() {
        AppointmentFilterDTO dto = new AppointmentFilterDTO(
                2L,
                null,
                null,
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 21),
                PageRequestDTO.builder()
                        .page(1)
                        .size(10)
                        .sortBy("unknown")
                        .sortDirection("ASC")
                        .build());

        InvalidSortFieldException ex = assertThrows(InvalidSortFieldException.class,
                () -> appointmentService.getAppointments(dto));

        assertEquals("unknown", ex.getMessage());
        verifyNoInteractions(doctorRepository, appointmentRepository);
    }

    @Test
    @DisplayName("getAppointments rejects invalid date ranges before touching the repository")
    void getAppointments_invalidDateRange() {
        AppointmentFilterDTO dto = new AppointmentFilterDTO(
                2L,
                null,
                null,
                LocalDate.of(2026, 7, 21),
                LocalDate.of(2026, 7, 20),
                PageRequestDTO.builder().page(1).size(10).sortDirection("ASC").build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.getAppointments(dto));

        assertEquals(ResponseCode.INVALID_DATE_RANGE.getCode(), ex.getMessage());
        verifyNoInteractions(doctorRepository, appointmentRepository);
    }

    @Test
    @DisplayName("getAppointments rejects ranges larger than 30 days before touching the repository")
    void getAppointments_rangeTooLarge() {
        AppointmentFilterDTO dto = new AppointmentFilterDTO(
                2L,
                null,
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 5),
                PageRequestDTO.builder().page(1).size(10).sortDirection("ASC").build());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appointmentService.getAppointments(dto));

        assertEquals(ResponseCode.DATE_RANGE_TOO_LARGE.getCode(), ex.getMessage());
        verifyNoInteractions(doctorRepository, appointmentRepository);
    }

    @Test
    @DisplayName("getAppointments throws when the doctor does not exist")
    void getAppointments_doctorNotFound() {
        AppointmentFilterDTO dto = new AppointmentFilterDTO(
                2L,
                null,
                null,
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 21),
                PageRequestDTO.builder().page(1).size(10).sortDirection("ASC").build());

        when(doctorRepository.findById(2L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.getAppointments(dto));

        assertEquals(ResponseCode.DOCTOR_NOT_FOUND.getCode(), ex.getMessage());
        verify(doctorRepository).findById(2L);
        verify(appointmentRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    private Appointment scheduledAppointment(Long id, LocalDateTime dateTime) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setAppointmentDateTime(dateTime);
        return appointment;
    }

    @Test
    @DisplayName("bookAppointment throws INVALID_WEEKDAY_SCHEDULE when appointment is before opening on a weekday")
    void bookAppointment_beforeOpeningWeekday() {

        LocalDate nextMonday = LocalDate.of(2026, 7, 17);

        AppointmentRequestDTO dto = new AppointmentRequestDTO(
                1L,
                2L,
                nextMonday.atTime(7, 0)
        );

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        when(doctorRepository.findById(2L))
                .thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> appointmentService.bookAppointment(dto));

        assertEquals(ResponseCode.INVALID_WEEKDAY_SCHEDULE.getCode(), ex.getMessage());

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("bookAppointment throws INVALID_SATURDAY_SCHEDULE when appointment is before opening on a weekday")
    void bookAppointment_beforeOpeningSaturday() {

        LocalDate nextMonday = LocalDate.of(2026, 7, 18);

        AppointmentRequestDTO dto = new AppointmentRequestDTO(
                1L,
                2L,
                nextMonday.atTime(17, 0)
        );

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        when(doctorRepository.findById(2L))
                .thenReturn(Optional.of(doctor));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> appointmentService.bookAppointment(dto));

        assertEquals(ResponseCode.INVALID_SATURDAY_SCHEDULE.getCode(), ex.getMessage());

        verify(appointmentRepository, never()).save(any());
    }
}
