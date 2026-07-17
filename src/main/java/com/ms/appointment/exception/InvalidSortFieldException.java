package com.ms.appointment.exception;

public class InvalidSortFieldException extends RuntimeException{
    public InvalidSortFieldException(String message) {
        super(message);
    }
}