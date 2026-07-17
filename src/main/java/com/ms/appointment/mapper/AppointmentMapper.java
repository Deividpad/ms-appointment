package com.ms.appointment.mapper;

import com.ms.appointment.dto.request.AppointmentRequestDTO;
import com.ms.appointment.dto.response.AppointmentResponseDTO;
import com.ms.appointment.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface AppointmentMapper {
    AppointmentMapper INSTANCE = Mappers.getMapper(AppointmentMapper.class);
    Appointment toEntity(AppointmentRequestDTO dto);

    //patient target
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientName", source = "patient.fullName")

    //doctor target
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.fullName")
    AppointmentResponseDTO toDto(Appointment appointment);

    List<AppointmentResponseDTO> toListDto(List<Appointment> appointment);
}
