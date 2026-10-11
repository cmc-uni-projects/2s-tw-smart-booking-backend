package com.example.smart_booking_system.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends AppException {

    public InternalServerException(String messageKey, Object... args) {
        super(messageKey, args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
