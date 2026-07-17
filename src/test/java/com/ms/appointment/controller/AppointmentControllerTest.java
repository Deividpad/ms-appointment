package com.ms.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.appointment.dto.request.AppointmentFilterDTO;
import com.ms.appointment.dto.request.AppointmentRequestDTO;
import com.ms.appointment.dto.request.RescheduleRequestDTO;
import com.ms.appointment.dto.response.AppointmentResponseDTO;
import com.ms.appointment.dto.response.AvailableDayResponseDTO;
import com.ms.appointment.dto.response.AvailableSlotDTO;
import com.ms.appointment.dto.response.PageResponse;
import com.ms.appointment.exception.GlobalExceptionHandler;
import com.ms.appointment.service.AppointmentService;
import com.ms.appointment.util.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppointmentController.class)
@Import(GlobalExceptionHandler.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("POST /appointment returns 201 and delegates to the service")
    void bookAppointment_success() throws Exception {
        AppointmentRequestDTO request = new AppointmentRequestDTO(
                10L,
                20L,
                LocalDateTime.of(2026, 7, 18, 9, 0));
        AppointmentResponseDTO response = new AppointmentResponseDTO(
                1L,
                10L,
                "John Doe",
                20L,
                "Dr. Smith",
                request.appointmentDateTime(),
                AppointmentStatus.SCHEDULED);

        when(appointmentService.bookAppointment(any(AppointmentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.patientId").value(10))
                .andExpect(jsonPath("$.patientName").value("John Doe"))
                .andExpect(jsonPath("$.doctorId").value(20))
                .andExpect(jsonPath("$.doctorName").value("Dr. Smith"))
                .andExpect(jsonPath("$.appointmentDateTime").value("2026-07-18T09:00:00"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(appointmentService).bookAppointment(eq(request));
    }

    @Test
    @DisplayName("POST /appointment rejects invalid body fields")
    void bookAppointment_validationError() throws Exception {
        String payload = """
                {
                  "patientId": null,
                  "doctorId": null,
                  "appointmentDateTime": null
                }
                """;

        mockMvc.perform(post("/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Patient ID is mandatory")))
                .andExpect(jsonPath("$.message", containsString("Doctor ID is mandatory")))
                .andExpect(jsonPath("$.message", containsString("Appointment date and time are mandatory")));
    }

    @Test
    @DisplayName("PUT /appointment/{id}/cancel returns 200 and delegates to the service")
    void cancelAppointment_success() throws Exception {
        AppointmentResponseDTO response = new AppointmentResponseDTO(
                1L,
                10L,
                "John Doe",
                20L,
                "Dr. Smith",
                LocalDateTime.of(2026, 7, 18, 9, 0),
                AppointmentStatus.CANCELED);

        when(appointmentService.cancelAppointment(1L)).thenReturn(response);

        mockMvc.perform(put("/appointment/{id}/cancel", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(appointmentService).cancelAppointment(1L);
    }

    @Test
    @DisplayName("PUT /appointment/{id}/reschedule returns 200 and delegates to the service")
    void rescheduleAppointment_success() throws Exception {
        RescheduleRequestDTO request = new RescheduleRequestDTO(LocalDateTime.of(2026, 7, 19, 10, 0));
        AppointmentResponseDTO response = new AppointmentResponseDTO(
                2L,
                10L,
                "John Doe",
                20L,
                "Dr. Smith",
                request.newAppointmentDateTime(),
                AppointmentStatus.SCHEDULED);

        when(appointmentService.rescheduleAppointment(eq(1L), any(RescheduleRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/appointment/{id}/reschedule", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.appointmentDateTime").value("2026-07-19T10:00:00"));

        verify(appointmentService).rescheduleAppointment(eq(1L), eq(request));
    }

    @Test
    @DisplayName("PUT /appointment/{id}/reschedule rejects missing new date")
    void rescheduleAppointment_validationError() throws Exception {
        mockMvc.perform(put("/appointment/{id}/reschedule", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New appointment date and time are mandatory"));
    }

    @Test
    @DisplayName("GET /appointment with a body returns a paged response")
    void listAppointments_success() throws Exception {
        PageResponse<AppointmentResponseDTO> response = new PageResponse<>();
        response.setData(List.of(new AppointmentResponseDTO(
                1L,
                10L,
                "John Doe",
                20L,
                "Dr. Smith",
                LocalDateTime.of(2026, 7, 18, 9, 0),
                AppointmentStatus.SCHEDULED)));
        response.setPage(1);
        response.setSize(10);
        response.setTotalElements(1);
        response.setTotalPages(1);
        response.setLast(true);

        when(appointmentService.getAppointments(any(AppointmentFilterDTO.class))).thenReturn(response);

        String payload = """
                {
                  "doctorId": 20,
                  "patientId": 10,
                  "status": "SCHEDULED",
                  "start": "2026-07-01",
                  "end": "2026-07-31",
                  "pageable": {
                    "page": 1,
                    "size": 10,
                    "sortBy": "appointmentDateTime",
                    "sortDirection": "ASC"
                  }
                }
                """;

        mockMvc.perform(get("/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));

        verify(appointmentService).getAppointments(any(AppointmentFilterDTO.class));
    }

    @Test
    @DisplayName("GET /appointment rejects invalid pagination values")
    void listAppointments_validationError() throws Exception {
        String payload = """
                {
                  "pageable": {
                    "page": 0,
                    "size": 101,
                    "sortDirection": "UP"
                  }
                }
                """;

        mockMvc.perform(get("/appointment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page must be greater than or equal to 1")))
                .andExpect(jsonPath("$.message", containsString("Size cannot be greater than 100")))
                .andExpect(jsonPath("$.message", containsString("Sort direction must be ASC or DESC")));
    }

    @Test
    @DisplayName("GET /appointment/availability returns available slots")
    void getAvailableSlots_success() throws Exception {
        List<AvailableDayResponseDTO> response = List.of(
                new AvailableDayResponseDTO(
                        LocalDate.of(2026, 7, 18),
                        List.of(new AvailableSlotDTO(1, LocalTime.of(8, 0), LocalTime.of(8, 30)))));

        when(appointmentService.getAvailableSlots(20L, LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 20)))
                .thenReturn(response);

        mockMvc.perform(get("/appointment/availability")
                        .param("doctorId", "20")
                        .param("startDate", "2026-07-18")
                        .param("endDate", "2026-07-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].date").value("2026-07-18"))
                .andExpect(jsonPath("$[0].slots", hasSize(1)))
                .andExpect(jsonPath("$[0].slots[0].slot").value(1))
                .andExpect(jsonPath("$[0].slots[0].start").value("08:00:00"))
                .andExpect(jsonPath("$[0].slots[0].end").value("08:30:00"));

        verify(appointmentService).getAvailableSlots(20L, LocalDate.of(2026, 7, 18), LocalDate.of(2026, 7, 20));
    }

    @Test
    @DisplayName("GET /appointment/availability rejects a past startDate")
    void getAvailableSlots_pastStartDate() throws Exception {
        mockMvc.perform(get("/appointment/availability")
                        .param("doctorId", "20")
                        .param("startDate", "2026-07-16")
                        .param("endDate", "2026-07-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Start date must be today or a future date")));
    }

    @Test
    @DisplayName("GET /appointment/availability rejects missing required params")
    void getAvailableSlots_missingParameter() throws Exception {
        mockMvc.perform(get("/appointment/availability")
                        .param("startDate", "2026-07-18")
                        .param("endDate", "2026-07-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required request parameter 'doctorId' of type 'Long' is missing."));
    }

    @Test
    @DisplayName("GET /appointment/availability rejects invalid parameter types")
    void getAvailableSlots_typeMismatch() throws Exception {
        mockMvc.perform(get("/appointment/availability")
                        .param("doctorId", "abc")
                        .param("startDate", "2026-07-18")
                        .param("endDate", "2026-07-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'doctorId'."));
    }
}
