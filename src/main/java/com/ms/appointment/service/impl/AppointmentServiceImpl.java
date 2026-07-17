package com.ms.appointment.service.impl;

import com.ms.appointment.dto.request.AppointmentFilterDTO;
import com.ms.appointment.dto.request.AppointmentRequestDTO;
import com.ms.appointment.dto.request.RescheduleRequestDTO;
import com.ms.appointment.dto.response.AppointmentResponseDTO;
import com.ms.appointment.dto.response.AvailableDayResponseDTO;
import com.ms.appointment.dto.response.AvailableSlotDTO;
import com.ms.appointment.dto.response.PageResponse;
import com.ms.appointment.entity.Appointment;
import com.ms.appointment.entity.Doctor;
import com.ms.appointment.entity.Patient;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.mapper.AppointmentMapper;
import com.ms.appointment.repository.*;
import com.ms.appointment.repository.impl.AppointmentSpecification;
import com.ms.appointment.service.AppointmentService;
import com.ms.appointment.service.PatientService;
import com.ms.appointment.service.PenaltyService;
import com.ms.appointment.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentMapper appointmentMapper = AppointmentMapper.INSTANCE;

    private final PenaltyService penaltyService;
    private final PatientService patientService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentServiceImpl(PenaltyService penaltyService, PatientService patientService, AppointmentRepository appointmentRepository, DoctorRepository doctorRepository,
                                  PatientRepository patientRepository) {
        this.penaltyService = penaltyService;
        this.patientService = patientService;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // RF-03: Book an Appointment
    @Override
    @Transactional
    public AppointmentResponseDTO bookAppointment(AppointmentRequestDTO dto) {
        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.PATIENT_NOT_FOUND.getCode()));

        patientService.validatePatientStatus(patient);

        Doctor doctor = getDoctor(dto.doctorId());

        validateAppointmentBusinessRules(patient, doctor, dto.appointmentDateTime());

        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.SCHEDULED)
                .appointmentDateTime(dto.appointmentDateTime())
                .build();

        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    // RF-05: Cancel an Appointment
    @Override
    @Transactional
    public AppointmentResponseDTO cancelAppointment(Long appointmentId) {

        Appointment appointment = getAppointment(appointmentId);

        patientService.validatePatientStatus(appointment.getPatient());

        LocalDateTime now = LocalDateTime.now();
        penaltyService.validatePenalty(appointment, now);

        appointment.setCanceledAt(now);
        appointment.setStatus(AppointmentStatus.CANCELED);
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    private Appointment getAppointment(Long appointmentId) {
        return appointmentRepository.findByIdAndStatus(appointmentId, AppointmentStatus.SCHEDULED)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.APPOINTMENT_NOT_FOUND.getCode()));
    }

    // RN-06: Reschedule an Appointment
    @Override
    @Transactional
    public AppointmentResponseDTO rescheduleAppointment(Long appointmentId, RescheduleRequestDTO dto) {

        Appointment oldAppointment = getAppointment(appointmentId);
        Patient patient = oldAppointment.getPatient();
        patientService.validatePatientStatus(oldAppointment.getPatient());

        Doctor doctor = oldAppointment.getDoctor();

        // 1. Cancel the current appointment (triggers RN-05 penalty assessment if late)
        LocalDateTime now = LocalDateTime.now();
        penaltyService.validatePenalty(oldAppointment, now);

        // 2. Validate that the new date and time are free & allowed (RN-02, RN-04, etc.)
        validateAppointmentBusinessRules(patient, doctor, dto.newAppointmentDateTime());


        oldAppointment.setStatus(AppointmentStatus.CANCELED);
        appointmentRepository.save(oldAppointment);

        // 3. Open a completely new scheduled record
        Appointment newAppointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .status(AppointmentStatus.SCHEDULED)
                .appointmentDateTime(dto.newAppointmentDateTime())
                .build();

        return appointmentMapper.toDto(appointmentRepository.save(newAppointment));
    }

    // RF-04: Check Available Slots for a Doctor
    @Override
    @Transactional(readOnly = true)
    public List<AvailableDayResponseDTO> getAvailableSlots(Long doctorId, LocalDate startDate, LocalDate endDate) {

        getDoctor(doctorId);
        DateUtil.validateDateRange(startDate, endDate);
        List<AvailableDayResponseDTO> result = new ArrayList<>();

        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            result.add(new AvailableDayResponseDTO(day, getAvailableSlotsForDay(day, doctorId)));
        }
        return result;

    }

    private List<AvailableSlotDTO> getAvailableSlotsForDay(LocalDate day, Long doctorId) {
        if (ClinicScheduleUtil.isClosed(day.getDayOfWeek()) || ClinicScheduleUtil.isHoliDay(day)) {
            return List.of();
        }

        Set<LocalDateTime> occupied = getOccupiedSlots(day, doctorId);
        return generateSlots(day, occupied);
    }

    private Set<LocalDateTime> getOccupiedSlots(LocalDate day, Long doctorId) {
        return appointmentRepository.findByDoctorIdAndAppointmentDateTimeBetweenAndStatus(doctorId, day.atStartOfDay(), day.atTime(LocalTime.MAX), AppointmentStatus.SCHEDULED)
                .stream()
                .map(Appointment::getAppointmentDateTime)
                .collect(Collectors.toSet());
    }

    private List<AvailableSlotDTO> generateSlots(LocalDate day, Set<LocalDateTime> occupied) {

        List<AvailableSlotDTO> result = new ArrayList<>();
        LocalTime slotTime = ClinicScheduleUtil.OPENING;
        LocalTime lastSlot = ClinicScheduleUtil.lastSlot(day.getDayOfWeek());

        int slotNumber = ClinicScheduleUtil.FIRST_AVAILABLE_SLOT;
        while (!slotTime.isAfter(lastSlot)) {
            LocalDateTime startDateTime = day.atTime(slotTime);

            if (!occupied.contains(startDateTime)) {
                result.add(new AvailableSlotDTO(
                        slotNumber,
                        slotTime,
                        slotTime.plusMinutes(ClinicScheduleUtil.SLOT_MINUTES)
                ));
            }

            slotTime = slotTime.plusMinutes(ClinicScheduleUtil.SLOT_MINUTES);
            slotNumber++;
        }
        return result;
    }

    private Doctor getDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.DOCTOR_NOT_FOUND.getCode()));
    }

    // RF-06: Query filtered lists
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponseDTO> getAppointments(AppointmentFilterDTO dto) {

        DateUtil.validateDateRange(dto.start(), dto.end());
        DateUtil.DateTimeRange dateTimeRange = DateUtil.toDateTimeRange(dto.start(), dto.end());
        Pageable pageable = PageableUtil.build(dto.pageable(),
                ColumnsFactory.AppointmentUtil.APPOINTMENT_DATE_TIME,
                ColumnsFactory.AppointmentUtil.FIELDS);

        getDoctor(dto.doctorId());

        Specification<Appointment> specification = Specification.allOf(
                AppointmentSpecification.hasDoctor(dto.doctorId()),
                AppointmentSpecification.hasPatient(dto.patientId()),
                AppointmentSpecification.hasStatus(dto.status()),
                AppointmentSpecification.appointmentFrom(dateTimeRange.startDateTime()),
                AppointmentSpecification.appointmentUntil(dateTimeRange.endDateTime())
        );
        return PageResponse.of(appointmentRepository.findAll(specification, pageable).map(appointmentMapper::toDto));
    }

    // --- Private Centralized Rule Validations ---
    private void validateAppointmentBusinessRules(Patient patient, Doctor doctor, LocalDateTime dateTime) {
        LocalDateTime cleanDate = dateTime.withSecond(0).withNano(0);
        patientService.validateBirthDate(patient);
        validateClinicSchedule(dateTime);
        validateDoctorAvailability(doctor.getId(), cleanDate);
        validatePatientAvailability(patient.getId(), cleanDate);
    }

    private void validateClinicSchedule(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        LocalTime time = dateTime.toLocalTime();

        if (time.getMinute() % ClinicScheduleUtil.SLOT_MINUTES != 0)
            throw new BusinessException(ResponseCode.INVALID_APPOINTMENT_TIME.getCode());

        if (ClinicScheduleUtil.isClosed(day))
            throw new BusinessException(ResponseCode.CLINIC_CLOSED_SUNDAY.getCode());

        if (ClinicScheduleUtil.isHoliDay(dateTime.toLocalDate()))
            throw new BusinessException(ResponseCode.CLINIC_IS_CLOSED_HOLIDAYS.getCode());

        LocalTime maxAllowedSlot = ClinicScheduleUtil.lastSlot(day);

        if (time.isBefore(ClinicScheduleUtil.OPENING) || time.isAfter(maxAllowedSlot)) {
            if (day == DayOfWeek.SATURDAY) {
                throw new BusinessException(ResponseCode.INVALID_SATURDAY_SCHEDULE.getCode());
            } else {
                throw new BusinessException(ResponseCode.INVALID_WEEKDAY_SCHEDULE.getCode());
            }
        }
    }

    private void validateDoctorAvailability(Long doctorId, LocalDateTime dateTime) {
        if (appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndStatus(doctorId, dateTime, AppointmentStatus.SCHEDULED)) {
            throw new BusinessException(ResponseCode.DOCTOR_ALREADY_BOOKED.getCode());
        }
    }

    private void validatePatientAvailability(Long patientId, LocalDateTime dateTime) {
        if (appointmentRepository.existsByPatientIdAndAppointmentDateTimeAndStatus(patientId, dateTime, AppointmentStatus.SCHEDULED)) {
            throw new BusinessException(ResponseCode.PATIENT_ALREADY_BOOKED.getCode());
        }
    }
}