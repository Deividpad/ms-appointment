package com.ms.appointment.service;

import com.ms.appointment.dto.request.AppointmentFilterDTO;
import com.ms.appointment.dto.request.AppointmentRequestDTO;
import com.ms.appointment.dto.request.RescheduleRequestDTO;
import com.ms.appointment.dto.response.AppointmentResponseDTO;
import com.ms.appointment.dto.response.AvailableDayResponseDTO;
import com.ms.appointment.dto.response.PageResponse;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentResponseDTO bookAppointment(AppointmentRequestDTO request);
    List<AvailableDayResponseDTO> getAvailableSlots(Long doctorId, LocalDate startDate, LocalDate endDate);
    AppointmentResponseDTO cancelAppointment(Long appointmentId);
    AppointmentResponseDTO rescheduleAppointment(Long appointmentId, RescheduleRequestDTO request);
    PageResponse<AppointmentResponseDTO> getAppointments(AppointmentFilterDTO dto);
}