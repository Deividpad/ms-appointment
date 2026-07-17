package com.ms.appointment.service;

import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;

public interface DoctorService {
    DoctorResponseDTO registerDoctor(DoctorRequestDTO request);
    DoctorResponseDTO getDoctorById(Long id);
}
