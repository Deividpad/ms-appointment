package com.ms.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.appointment.dto.request.PatientRequestDTO;
import com.ms.appointment.dto.response.PatientResponseDTO;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.GlobalExceptionHandler;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PatientController.class)
@Import(GlobalExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    @Test
    @DisplayName("POST /patient returns 201 and delegates to the service")
    void registerPatient_success() throws Exception {
        PatientRequestDTO request = new PatientRequestDTO(
                "John Doe",
                "12345678",
                "3001234567",
                "john@example.com",
                LocalDate.of(1990, 1, 1));
        PatientResponseDTO response = new PatientResponseDTO(
                1L,
                "John Doe",
                "12345678",
                "3001234567",
                "john@example.com",
                LocalDate.of(1990, 1, 1));

        when(patientService.registerPatient(any(PatientRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.identityDocument").value("12345678"))
                .andExpect(jsonPath("$.phone").value("3001234567"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"));

        verify(patientService).registerPatient(eq(request));
    }

    @Test
    @DisplayName("POST /patient rejects an invalid payload")
    void registerPatient_validationError() throws Exception {
        String payload = """
                {
                  "fullName": "",
                  "identityDocument": "",
                  "phone": "123",
                  "email": "bad-email",
                  "birthDate": null
                }
                """;

        mockMvc.perform(post("/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Full name is mandatory")))
                .andExpect(jsonPath("$.message", containsString("Identity document is mandatory")))
                .andExpect(jsonPath("$.message", containsString("Phone must have at least 7 digits")))
                .andExpect(jsonPath("$.message", containsString("Email must be a valid email format")))
                .andExpect(jsonPath("$.message", containsString("Birth date is mandatory")));
    }

    @Test
    @DisplayName("POST /patient rejects a future birth date")
    void registerPatient_futureBirthDate() throws Exception {
        PatientRequestDTO request = new PatientRequestDTO(
                "John Doe",
                "12345678",
                "3001234567",
                "john@example.com",
                LocalDate.now().plusDays(1));

        mockMvc.perform(post("/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Birth date must be before today")));
    }

    @Test
    @DisplayName("POST /patient rejects a malformed birthDate value")
    void registerPatient_invalidBirthDateFormat() throws Exception {
        String payload = """
                {
                  "fullName": "John Doe",
                  "identityDocument": "12345678",
                  "phone": "3001234567",
                  "email": "john@example.com",
                  "birthDate": "not-a-date"
                }
                """;

        mockMvc.perform(post("/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for field 'birthDate'."));
    }

    @Test
    @DisplayName("POST /patient propagates business exceptions from the service")
    void registerPatient_businessException() throws Exception {
        PatientRequestDTO request = new PatientRequestDTO(
                "John Doe",
                "12345678",
                "3001234567",
                "john@example.com",
                LocalDate.of(1990, 1, 1));

        when(patientService.registerPatient(any(PatientRequestDTO.class)))
                .thenThrow(new BusinessException("Phone already exists"));

        mockMvc.perform(post("/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phone already exists"));
    }

    @Test
    @DisplayName("GET /patient/{id} returns 200 and delegates to the service")
    void getPatientById_success() throws Exception {
        PatientResponseDTO response = new PatientResponseDTO(
                1L,
                "John Doe",
                "12345678",
                "3001234567",
                "john@example.com",
                LocalDate.of(1990, 1, 1));

        when(patientService.getPatientById(1L)).thenReturn(response);

        mockMvc.perform(get("/patient/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.identityDocument").value("12345678"))
                .andExpect(jsonPath("$.birthDate").value("1990-01-01"));

        verify(patientService).getPatientById(1L);
    }

    @Test
    @DisplayName("GET /patient/{id} returns 404 when the patient does not exist")
    void getPatientById_notFound() throws Exception {
        when(patientService.getPatientById(99L))
                .thenThrow(new ResourceNotFoundException("Patient not found"));

        mockMvc.perform(get("/patient/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Patient not found"));
    }

    @Test
    @DisplayName("GET /patient/{id} rejects invalid path variables")
    void getPatientById_typeMismatch() throws Exception {
        mockMvc.perform(get("/patient/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'."));
    }
}
