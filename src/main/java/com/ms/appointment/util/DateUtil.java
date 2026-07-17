package com.ms.appointment.util;

import com.ms.appointment.exception.BusinessException;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateUtil {
    private static final long MAX_RANGE_DATE = 30;

    public static void validateDateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null)
            return;

        if (end.isBefore(start))
            throw new BusinessException(ResponseCode.INVALID_DATE_RANGE.getCode());

        if (start.plusDays(MAX_RANGE_DATE).isBefore(end))
            throw new BusinessException(ResponseCode.DATE_RANGE_TOO_LARGE.getCode());
    }

    public record DateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {}

    public static DateTimeRange toDateTimeRange(LocalDate start, LocalDate end) {
        return new DateTimeRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
    }
}
