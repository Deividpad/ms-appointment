package com.ms.appointment.mapper;

import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.request.PatientRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;
import com.ms.appointment.dto.response.PatientResponseDTO;
import com.ms.appointment.entity.Doctor;
import com.ms.appointment.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PatientMapper {
    PatientMapper INSTANCE = Mappers.getMapper(PatientMapper.class);
    Patient toEntity(PatientRequestDTO dto);
    PatientResponseDTO toDto(Patient entity);
}
