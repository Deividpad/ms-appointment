package com.ms.appointment.util;

import com.ms.appointment.dto.request.AppointmentFilterDTO;
import com.ms.appointment.dto.request.PageRequestDTO;
import com.ms.appointment.exception.BusinessException;
import com.ms.appointment.exception.InvalidSortFieldException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public class PageableUtil {

    public static Pageable build(PageRequestDTO dto, String defaultSortBy, Map<String, String> allowedFields) {

        String sortBy = dto.getSortBy();

        if (sortBy == null || sortBy.isBlank()) {
            sortBy = defaultSortBy;
        }

        String entityField = allowedFields.get(sortBy);
        if (entityField == null) {
            throw new InvalidSortFieldException(sortBy);
        }

        Sort sort = Sort.by(Sort.Direction.fromString(dto.getSortDirection()), entityField);

        return PageRequest.of(dto.getPage() - 1, dto.getSize(), sort);
    }
}