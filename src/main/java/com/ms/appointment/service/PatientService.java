package com.ms.appointment.service;

import com.ms.appointment.dto.request.PatientRequestDTO;
import com.ms.appointment.dto.response.PatientResponseDTO;
import com.ms.appointment.entity.Patient;

public interface PatientService {
    PatientResponseDTO getPatientById(Long id);
    PatientResponseDTO registerPatient(PatientRequestDTO request);
    void validatePatientStatus(Patient patient);
    void validateBirthDate(Patient patient);
}
