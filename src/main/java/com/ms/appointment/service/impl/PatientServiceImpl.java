package com.ms.appointment.service.impl;

import com.ms.appointment.dto.request.PatientRequestDTO;
import com.ms.appointment.dto.response.PatientResponseDTO;
import com.ms.appointment.entity.Patient;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.mapper.PatientMapper;
import com.ms.appointment.repository.PatientRepository;
import com.ms.appointment.service.PatientService;
import com.ms.appointment.util.PatientStatus;
import com.ms.appointment.util.ResponseCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientMapper patientMapper = PatientMapper.INSTANCE;

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponseDTO registerPatient(PatientRequestDTO dto) {

        if (patientRepository.existsByPhone(dto.phone()))
            throw new BusinessException(ResponseCode.PHONE_ALREADY_EXISTS.getCode());

        if (patientRepository.existsByEmail(dto.email()))
            throw new BusinessException(ResponseCode.EMAIL_ALREADY_EXISTS.getCode());

        if (patientRepository.existsByIdentityDocument(dto.identityDocument()))
            throw new BusinessException(ResponseCode.DOCUMENT_ALREADY_EXISTS.getCode());

        Patient patient = patientMapper.toEntity(dto);
        patient.setStatus(PatientStatus.AVAILABLE);
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.PATIENT_NOT_FOUND.getCode()));
        return patientMapper.toDto(patient);
    }

    public void validateBirthDate(Patient patient) {
        LocalDate birthDate = patient.getBirthDate() != null ? patient.getBirthDate() : LocalDate.now();
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ResponseCode.INVALID_BIRTH_DATE.getCode());
        }
    }

    public void validatePatientStatus(Patient patient){
        if (PatientStatus.PENALTY_BLOCKED.equals(patient.getStatus()))
            throw new BusinessException(ResponseCode.PATIENT_PENALTY_BLOCKED.getCode() + " until " + patient.getUnlockPenaltyDate());
    }

}