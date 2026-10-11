package com.example.smart_booking_system.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * High-frequency methods (polling endpoints, every-minute jobs) whose normal calls would flood INFO.
 * {@link LoggingAspect} logs them at DEBUG; failures and slow calls are still logged at WARN/ERROR.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuietLogging {
}
