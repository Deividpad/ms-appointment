package com.ms.appointment.service.impl;

import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;
import com.ms.appointment.entity.Doctor;
import com.ms.appointment.entity.Specialty;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.ResourceNotFoundException;
import com.ms.appointment.mapper.DoctorMapper;
import com.ms.appointment.repository.DoctorRepository;
import com.ms.appointment.repository.SpecialtyRepository;
import com.ms.appointment.service.DoctorService;
import com.ms.appointment.util.ResponseCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorMapper doctorMapper = DoctorMapper.INSTANCE;

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository, SpecialtyRepository specialtyRepository) {
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
    }

    @Transactional
    public DoctorResponseDTO registerDoctor(DoctorRequestDTO dto) {

        if (doctorRepository.existsByPhone(dto.phone()))
            throw new BusinessException(ResponseCode.PHONE_ALREADY_EXISTS.getCode());

        if (doctorRepository.existsByEmail(dto.email()))
            throw new BusinessException(ResponseCode.EMAIL_ALREADY_EXISTS.getCode());

        Specialty specialty = specialtyRepository.findById(dto.specialtyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(ResponseCode.SPECIALTY_NOT_FOUND.getCode()));

        Doctor doctor = doctorMapper.toEntity(dto);
        doctor.setSpecialty(specialty);
        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public DoctorResponseDTO getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(ResponseCode.DOCTOR_NOT_FOUND.getCode()));
        return doctorMapper.toDto(doctor);
    }
}