package com.ms.appointment.util;

import java.util.Map;

public class ColumnsFactory {

    public static class AppointmentUtil {
        public static final String APPOINTMENT_DATE_TIME = "appointmentDateTime";
        public static final String PATIENT_NAME = "patientName";
        public static final String PATIENT_NAME_ENTITY = "patient.fullName";

        public static final Map<String, String> FIELDS = Map.of(
                APPOINTMENT_DATE_TIME, APPOINTMENT_DATE_TIME,
                PATIENT_NAME, PATIENT_NAME_ENTITY
        );
    }
}
