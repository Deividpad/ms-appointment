package com.ms.appointment.util;

import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ClinicScheduleUtil {

    public static final int FIRST_AVAILABLE_SLOT = 1;
    public final static int RANGE_DAYS_FOR_BLOCK = 30;
    public final static int MAX_PENALTIES_PATIENT = 3;
    private static final String COUNTRY_HOLI_DAYS = "CO";

    public static final LocalTime OPENING = LocalTime.of(8,0);
    public static final LocalTime WEEKDAY_LAST_SLOT = LocalTime.of(17,30);
    public static final LocalTime SATURDAY_LAST_SLOT = LocalTime.of(12,30);
    public static final int SLOT_MINUTES = 30;

    public static boolean isClosed(DayOfWeek day) {
        return day == DayOfWeek.SUNDAY;
    }

    public static LocalTime lastSlot(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY
                ? SATURDAY_LAST_SLOT
                : WEEKDAY_LAST_SLOT;
    }

    public static LocalDateTime subtractDaysToPenaltyDateValidation(LocalDateTime now){
        return now.minusDays(RANGE_DAYS_FOR_BLOCK);
    }

    public static boolean isHoliDay(LocalDate appointment){
        HolidayManager manager = HolidayManager.getInstance(ManagerParameters.create(COUNTRY_HOLI_DAYS));
        return manager.isHoliday(appointment);
    }
}
