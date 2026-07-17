package com.ms.appointment.mapper;

import com.ms.appointment.dto.request.DoctorRequestDTO;
import com.ms.appointment.dto.response.DoctorResponseDTO;
import com.ms.appointment.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DoctorMapper {
    DoctorMapper INSTANCE = Mappers.getMapper(DoctorMapper.class);

    @Mapping(target = "specialty.id", source = "specialtyId")
    Doctor toEntity(DoctorRequestDTO dto);

    @Mapping(target = "specialty", source = "specialty.name")
    DoctorResponseDTO toDto(Doctor entity);
}
