package com.example.smart_booking_system.i18n;

import com.example.smart_booking_system.exception.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Small facade over MessageSource: t(key, args) in the current request language. */
@Component
public class Messages {

    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    /** Prefix of texts stored as "i18n:<key>:<json args>" (rendered when read, in the reader's language). */
    private static final String DEFERRED = "i18n:";

    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public Messages(MessageSource messageSource, ObjectMapper objectMapper) {
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    /** Translate in the current request locale (Accept-Language). */
    public String t(String key, Object... args) {
        return t(LocaleContextHolder.getLocale(), key, args);
    }

    /** Translate in an explicit locale (emails, notifications for another user). */
    public String t(Locale locale, String key, Object... args) {
        if (key == null) return null;
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return key; // literal text or unknown key: show as-is
        }
    }

    /**
     * User-facing message for an exception caught in a controller: translated if it carries an i18n key;
     * anything else is unexpected -> logged with its stack trace and answered with a generic message
     * (internal details never reach the client).
     */
    public String of(Throwable ex) {
        if (ex instanceof AppException app) return t(app.getMessageKey(), app.getArgs());
        log.error("Unexpected error handled in controller", ex);
        return t("error.unexpected");
    }

    /** Encode key + args for storage (e.g. audit-log descriptions); see {@link #render(String)}. */
    public String deferred(String key, Object... args) {
        try {
            return DEFERRED + key + ":" + objectMapper.writeValueAsString(args == null ? new Object[0] : args);
        } catch (JsonProcessingException e) {
            return t(key, args);
        }
    }

    /** Render a stored text: "i18n:key:[args]" is translated in the request locale, anything else returned as-is. */
    public String render(String stored) {
        if (stored == null || !stored.startsWith(DEFERRED)) return stored;
        int sep = stored.indexOf(':', DEFERRED.length());
        if (sep < 0) return stored;
        String key = stored.substring(DEFERRED.length(), sep);
        try {
            return t(key, objectMapper.readValue(stored.substring(sep + 1), Object[].class));
        } catch (JsonProcessingException e) {
            return t(key);
        }
    }
}
