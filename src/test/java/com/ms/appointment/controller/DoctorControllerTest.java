package com.ms.appointment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;
import com.ms.appointment.exception.GlobalExceptionHandler;
import com.ms.appointment.service.DoctorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DoctorController.class)
@Import(GlobalExceptionHandler.class)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DoctorService doctorService;

    @Test
    @DisplayName("POST /doctor returns 201 and delegates to the service")
    void registerDoctor_success() throws Exception {
        DoctorRequestDTO request = new DoctorRequestDTO(
                "Dr. Alice Smith",
                5L,
                "3001234567",
                "alice@example.com");
        DoctorResponseDTO response = new DoctorResponseDTO(
                1L,
                "Dr. Alice Smith",
                "Cardiology",
                "3001234567",
                "alice@example.com");

        when(doctorService.registerDoctor(any(DoctorRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Dr. Alice Smith"))
                .andExpect(jsonPath("$.specialty").value("Cardiology"))
                .andExpect(jsonPath("$.phone").value("3001234567"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        verify(doctorService).registerDoctor(eq(request));
    }

    @Test
    @DisplayName("POST /doctor rejects an invalid payload")
    void registerDoctor_validationError() throws Exception {
        String payload = """
                {
                  "fullName": "",
                  "specialtyId": 0,
                  "phone": "123",
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Full name is mandatory")))
                .andExpect(jsonPath("$.message", containsString("Specialty Id must be greater than zero")))
                .andExpect(jsonPath("$.message", containsString("Phone must have at least 7 digits")))
                .andExpect(jsonPath("$.message", containsString("Email must be a valid email format")));
    }

    @Test
    @DisplayName("POST /doctor rejects a missing specialtyId")
    void registerDoctor_missingSpecialtyId() throws Exception {
        String payload = """
                {
                  "fullName": "Dr. Alice Smith",
                  "phone": "3001234567",
                  "email": "alice@example.com"
                }
                """;

        mockMvc.perform(post("/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Specialty Id is mandatory")));
    }

    @Test
    @DisplayName("GET /doctor/{id} returns 200 and delegates to the service")
    void getDoctorById_success() throws Exception {
        DoctorResponseDTO response = new DoctorResponseDTO(
                1L,
                "Dr. Alice Smith",
                "Cardiology",
                "3001234567",
                "alice@example.com");

        when(doctorService.getDoctorById(1L)).thenReturn(response);

        mockMvc.perform(get("/doctor/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Dr. Alice Smith"))
                .andExpect(jsonPath("$.specialty").value("Cardiology"));

        verify(doctorService).getDoctorById(1L);
    }

    @Test
    @DisplayName("GET /doctor/{id} returns 404 when the doctor does not exist")
    void getDoctorById_notFound() throws Exception {
        when(doctorService.getDoctorById(99L)).thenThrow(new com.ms.appointment.exception.ResourceNotFoundException("Doctor not found"));

        mockMvc.perform(get("/doctor/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor not found"));
    }

    @Test
    @DisplayName("GET /doctor/{id} rejects invalid path variables")
    void getDoctorById_typeMismatch() throws Exception {
        mockMvc.perform(get("/doctor/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'."));
    }
}
