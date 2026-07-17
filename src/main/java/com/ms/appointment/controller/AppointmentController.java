package com.ms.appointment.controller;

import com.ms.appointment.dto.request.AppointmentFilterDTO;
import com.ms.appointment.dto.request.AppointmentRequestDTO;
import com.ms.appointment.dto.request.RescheduleRequestDTO;
import com.ms.appointment.dto.response.AppointmentResponseDTO;
import com.ms.appointment.dto.response.AvailableDayResponseDTO;
import com.ms.appointment.dto.response.PageResponse;
import com.ms.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // RF-03: Book an Appointment
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> bookAppointment(@Valid @RequestBody AppointmentRequestDTO request) {
        AppointmentResponseDTO response = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // RF-05: Cancel an Appointment
    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@PathVariable Long id) {
        AppointmentResponseDTO response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }

    // RN-06: Reschedule an Appointment
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponseDTO> rescheduleAppointment(@PathVariable Long id, @Valid @RequestBody RescheduleRequestDTO request) {

        AppointmentResponseDTO response = appointmentService.rescheduleAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    // RF-06: List Appointments with optional filters
    @GetMapping
    public ResponseEntity<PageResponse<AppointmentResponseDTO>> listAppointments(@Valid @RequestBody AppointmentFilterDTO dto) {
        return ResponseEntity.ok(appointmentService.getAppointments(dto));
    }

    // RF-04: Check Available Slots for a Doctor
    @GetMapping("/availability")
    public ResponseEntity<List<AvailableDayResponseDTO>> getAvailableSlots(@RequestParam Long doctorId,
                                                                           @RequestParam @FutureOrPresent(message = "Start date must be today or a future date") LocalDate startDate,
                                                                           @RequestParam LocalDate endDate) {

        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, startDate, endDate));
    }
}