package com.example.smart_booking_system.logging;

import com.example.smart_booking_system.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Cross-cutting logging (levels are per class, so they can be tuned with logging.level.*):
 * <ul>
 *   <li>controllers – one INFO line per request: method, path, handler, user, status, duration;
 *       business errors ({@link AppException}) at WARN, unexpected ones at ERROR (stack trace is
 *       logged once, by the GlobalExceptionHandler);</li>
 *   <li>services – DEBUG on entry (masked args) and exit; WARN when slower than
 *       {@code app.logging.slow-threshold-ms}; DEBUG when an exception passes through;</li>
 *   <li>@Scheduled jobs – INFO start/finish with duration, ERROR on failure.</li>
 * </ul>
 */
@Aspect
@Component
public class LoggingAspect {

    private final LogSanitizer sanitizer;
    private final long slowThresholdMs;

    public LoggingAspect(ObjectMapper objectMapper,
                         @Value("${app.logging.slow-threshold-ms:1000}") long slowThresholdMs) {
        this.sanitizer = new LogSanitizer(objectMapper);
        this.slowThresholdMs = slowThresholdMs;
    }

    @Pointcut("within(com.example.smart_booking_system..*) && @within(org.springframework.web.bind.annotation.RestController)")
    void controllers() {}

    @Pointcut("within(com.example.smart_booking_system.service..*)")
    void services() {}

    @Pointcut("within(com.example.smart_booking_system..*) && @annotation(org.springframework.scheduling.annotation.Scheduled)")
    void scheduledJobs() {}

    @Around("controllers()")
    public Object logRequest(ProceedingJoinPoint pjp) throws Throwable {
        Logger log = logger(pjp);
        HttpServletRequest request = currentRequest();
        String route = request == null ? "-" : request.getMethod() + " " + request.getRequestURI();
        String handler = handlerName(pjp);
        String user = currentUser();
        if (request != null) request.setAttribute(RequestIdFilter.LOGGED_ATTRIBUTE, Boolean.TRUE);
        if (log.isDebugEnabled()) log.debug("{} -> {}({})", route, handler, args(pjp));

        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long ms = elapsedMs(start);
            int status = result instanceof ResponseEntity<?> re ? re.getStatusCode().value() : 200;
            if (status >= 400) log.warn("{} -> {} {} in {}ms user={}", route, handler, status, ms, user);
            else if (ms > slowThresholdMs) log.warn("{} -> {} {} in {}ms user={} (slow)", route, handler, status, ms, user);
            else log.info("{} -> {} {} in {}ms user={}", route, handler, status, ms, user);
            return result;
        } catch (AppException ex) {
            log.warn("{} -> {} {} in {}ms user={} [{}: {}]", route, handler, ex.getStatus().value(), elapsedMs(start), user,
                    ex.getClass().getSimpleName(), ex.getMessageKey());
            throw ex;
        } catch (Throwable ex) {
            log.error("{} -> {} failed in {}ms user={} [{}: {}]", route, handler, elapsedMs(start), user,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    @Around("services()")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        Logger log = logger(pjp);
        String method = handlerName(pjp);
        if (log.isDebugEnabled()) log.debug("→ {}({})", method, args(pjp));
        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long ms = elapsedMs(start);
            if (ms > slowThresholdMs) log.warn("{} took {}ms (slow)", method, ms);
            else if (log.isDebugEnabled()) log.debug("← {} = {} ({}ms)", method, sanitizer.value(result), ms);
            return result;
        } catch (Throwable ex) {
            if (log.isDebugEnabled()) log.debug("✗ {} threw {}: {} ({}ms)", method, ex.getClass().getSimpleName(), ex.getMessage(), elapsedMs(start));
            throw ex;
        }
    }

    @Around("scheduledJobs()")
    public Object logScheduledJob(ProceedingJoinPoint pjp) throws Throwable {
        Logger log = logger(pjp);
        String job = handlerName(pjp);
        long start = System.nanoTime();
        log.debug("Job {} started", job);
        try {
            Object result = pjp.proceed();
            long ms = elapsedMs(start);
            if (ms > slowThresholdMs) log.warn("Job {} finished in {}ms (slow)", job, ms);
            else log.debug("Job {} finished in {}ms", job, ms);
            return result;
        } catch (Throwable ex) {
            log.error("Job {} failed after {}ms", job, elapsedMs(start), ex);
            throw ex;
        }
    }

    // ------------------------------------------------------------------ helpers

    private String args(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sanitizer.args(sig.getParameterNames(), pjp.getArgs());
    }

    private static Logger logger(ProceedingJoinPoint pjp) {
        return LoggerFactory.getLogger(pjp.getTarget().getClass());
    }

    private static String handlerName(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getSimpleName() + "." + pjp.getSignature().getName();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs ? attrs.getRequest() : null;
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return "anonymous";
        return auth.getName();
    }
}
