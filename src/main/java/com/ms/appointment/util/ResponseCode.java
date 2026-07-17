package com.ms.appointment.util;

public enum ResponseCode {
    RESOURCE_NOT_FOUND("Resource not found"),
    INVALID_DATE_RANGE("Invalid Range Date"),
    DATE_RANGE_TOO_LARGE("Date range too large. Max 30 days"),
    INVALID_SORT_FIELD_EXCEPTION("Invalid sort field exception"),
    DOCUMENT_ALREADY_EXISTS("Document already exists"),
    PATIENT_NOT_FOUND("Patient not found"),
    SPECIALTY_NOT_FOUND("Specialty not found"),
    PHONE_ALREADY_EXISTS("Phone already exists"),
    EMAIL_ALREADY_EXISTS("Email already exists"),
    APPOINTMENT_NOT_FOUND("Appointment not found"),
    DOCTOR_NOT_FOUND("Doctor not found"),
    INVALID_BIRTH_DATE("Invalid birth date"),
    PATIENT_PENALTY_BLOCKED("Patient temporally blocked"),
    INVALID_APPOINTMENT_TIME("Appointments must be scheduled in 30-minute intervals"),
    CLINIC_CLOSED_SUNDAY("Clinic is closed on Sundays"),
    CLINIC_IS_CLOSED_HOLIDAYS("Clinic is closed on Holidays"),
    INVALID_SATURDAY_SCHEDULE("Saturday appointments are allowed only between 08:00 and 13:00"),
    INVALID_WEEKDAY_SCHEDULE("Weekday appointments are allowed only between 08:00 and 18:00"),
    PATIENT_BLOCKED_BY_PENALTIES("Patient has 3 or more penalties in the last 30 days"),
    DOCTOR_ALREADY_BOOKED("Doctor already has an appointment at the selected time"),
    PATIENT_ALREADY_BOOKED("Patient already has an appointment at the selected time");

    private final String code;

    ResponseCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
