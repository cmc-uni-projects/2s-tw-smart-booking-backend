package com.example.smart_booking_system.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for business errors. The message is an i18n key (resources/i18n/messages*.properties)
 * plus arguments; GlobalExceptionHandler translates it into the request language.
 * A literal sentence is still accepted and shown as-is when no key matches.
 */
public abstract class AppException extends RuntimeException {

    private final transient Object[] args;

    protected AppException(String messageKey, Object... args) {
        super(messageKey);
        this.args = args == null ? new Object[0] : args;
    }

    public String getMessageKey() {
        return getMessage();
    }

    public Object[] getArgs() {
        return args.clone();
    }

    public abstract HttpStatus getStatus();
}
