package com.example.smart_booking_system.logging;

import com.example.smart_booking_system.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Cross-cutting logging with inputs and outputs (levels are per class: tune with logging.level.*).
 * <pre>
 * → POST /api/v1/bookings BookingController.createBooking(request=BookingRequestDTO{roomId=3, …}) user=customer@travelmate.vn
 *   → BookingService.createBooking(request=…, userId="…")
 *   ← BookingService.createBooking = BookingResponseDTO{bookingId=14, status=PENDING_PAYMENT, …} (21ms)
 * ← POST /api/v1/bookings 200 in 35ms result=ResponseEntity[200] ApiResponse{…}
 * </pre>
 * <ul>
 *   <li>controllers: INFO request line (masked arguments) and response line (status, duration, result);
 *       WARN for status ≥ 400, slow calls and business errors ({@link AppException}), ERROR for unexpected ones
 *       (the stack trace is logged once, by GlobalExceptionHandler);</li>
 *   <li>services: INFO entry (arguments) and exit (return value, duration), indented by call depth;
 *       WARN when slower than {@code app.logging.slow-threshold-ms};</li>
 *   <li>@Scheduled jobs: INFO start/finish, ERROR on failure;</li>
 *   <li>{@link QuietLogging} methods (polling, every-minute jobs): the same lines at DEBUG.</li>
 * </ul>
 * Values are rendered by {@link LogSanitizer}: secrets masked, entities without lazy loading, text truncated
 * to {@code app.logging.max-length}.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private final LogSanitizer sanitizer;
    private final long slowThresholdMs;

    public LoggingAspect(@Value("${app.logging.slow-threshold-ms:1000}") long slowThresholdMs,
                         @Value("${app.logging.max-length:2000}") int maxLength) {
        this.sanitizer = new LogSanitizer(maxLength);
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
        String route = request == null ? "-" : request.getMethod() + " " + request.getRequestURI() + query(request);
        String handler = handlerName(pjp);
        String user = currentUser();
        Level normal = quiet(pjp) ? Level.DEBUG : Level.INFO;
        if (request != null) request.setAttribute(RequestIdFilter.LOGGED_ATTRIBUTE, Boolean.TRUE);
        if (log.isEnabledForLevel(normal)) log.atLevel(normal).log("→ {} {}({}) user={}", route, handler, args(pjp), user);

        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long ms = elapsedMs(start);
            int status = result instanceof ResponseEntity<?> re ? re.getStatusCode().value() : 200;
            Level level = status >= 400 || ms > slowThresholdMs ? Level.WARN : normal;
            if (log.isEnabledForLevel(level)) {
                log.atLevel(level).log("← {} {} in {}ms{} result={}", route, status, ms, ms > slowThresholdMs ? " (slow)" : "",
                        returnValue(pjp, result));
            }
            return result;
        } catch (AppException ex) {
            log.warn("✗ {} {} in {}ms [{}: {}] args=({}) user={}", route, ex.getStatus().value(), elapsedMs(start),
                    ex.getClass().getSimpleName(), ex.getMessageKey(), args(pjp), user);
            throw ex;
        } catch (Throwable ex) {
            log.error("✗ {} failed in {}ms [{}: {}] args=({}) user={}", route, elapsedMs(start),
                    ex.getClass().getSimpleName(), ex.getMessage(), args(pjp), user);
            throw ex;
        }
    }

    @Around("services()")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        Logger log = logger(pjp);
        String method = handlerName(pjp);
        Level normal = quiet(pjp) ? Level.DEBUG : Level.INFO;
        int depth = DEPTH.get();
        String indent = "  ".repeat(depth + 1);
        if (log.isEnabledForLevel(normal)) log.atLevel(normal).log("{}→ {}({})", indent, method, args(pjp));
        DEPTH.set(depth + 1);
        long start = System.nanoTime();
        try {
            Object result = pjp.proceed();
            long ms = elapsedMs(start);
            Level level = ms > slowThresholdMs ? Level.WARN : normal;
            if (log.isEnabledForLevel(level)) {
                log.atLevel(level).log("{}← {} = {} ({}ms{})", indent, method, returnValue(pjp, result), ms, ms > slowThresholdMs ? ", slow" : "");
            }
            return result;
        } catch (AppException ex) {
            // expected business outcome; the controller line logs it as WARN
            if (log.isEnabledForLevel(normal)) {
                log.atLevel(normal).log("{}✗ {} rejected [{}: {}] ({}ms)", indent, method, ex.getClass().getSimpleName(), ex.getMessageKey(), elapsedMs(start));
            }
            throw ex;
        } catch (Throwable ex) {
            log.warn("{}✗ {} threw {}: {} ({}ms) args=({})", indent, method, ex.getClass().getSimpleName(), ex.getMessage(), elapsedMs(start), args(pjp));
            throw ex;
        } finally {
            if (depth == 0) DEPTH.remove(); else DEPTH.set(depth);
        }
    }

    @Around("scheduledJobs()")
    public Object logScheduledJob(ProceedingJoinPoint pjp) throws Throwable {
        Logger log = logger(pjp);
        String job = handlerName(pjp);
        Level normal = quiet(pjp) ? Level.DEBUG : Level.INFO;
        long start = System.nanoTime();
        log.atLevel(normal).log("Job {} started", job);
        try {
            Object result = pjp.proceed();
            long ms = elapsedMs(start);
            log.atLevel(ms > slowThresholdMs ? Level.WARN : normal).log("Job {} finished in {}ms{}", job, ms, ms > slowThresholdMs ? " (slow)" : "");
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

    private String returnValue(ProceedingJoinPoint pjp, Object result) {
        Class<?> type = ((MethodSignature) pjp.getSignature()).getReturnType();
        return type == void.class ? "(void)" : sanitizer.value(result);
    }

    private static boolean quiet(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        return sig.getMethod().isAnnotationPresent(QuietLogging.class)
                || pjp.getTarget().getClass().isAnnotationPresent(QuietLogging.class);
    }

    private static String query(HttpServletRequest request) {
        String q = request.getQueryString();
        if (q == null) return "";
        // tokens in query strings (verify-email, reset-password links) must not reach the logs
        return "?" + q.replaceAll("(?i)((?:[a-z]*token|code|password)=)[^&]*", "$1" + LogSanitizer.MASK);
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
