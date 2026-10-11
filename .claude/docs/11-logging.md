# Logging (Spring AOP)

Instead of `log.info` calls scattered through every method, one **aspect** (`logging/LoggingAspect.java`) wraps controllers, services and scheduled jobs and logs around them. Plain SLF4J (`@Slf4j` / `LoggerFactory`) is still used for business events (e.g. `Email 'email/checkin-reminder' sent to … (en)`).

## What gets logged

| Layer (pointcut) | INFO | WARN | ERROR | DEBUG |
|---|---|---|---|---|
| Controllers (`@RestController`) | one line per request: `GET /api/v1/notifications -> NotificationController.getNotifications 200 in 22ms user=admin@travelmate.vn` | status ≥ 400, slow requests, `AppException` (with its message key) | unexpected exceptions | arguments |
| Services (`..service..`) | n/a | calls slower than the threshold | n/a | `→ method(args)`, `← method = result (ms)`, `✗ method threw …` |
| Jobs (`@Scheduled`) | n/a | slow runs | failures with stack trace | start / finish |

`RequestIdFilter` (first filter in the chain) puts a request id in the MDC (taken from an incoming `X-Request-Id` or 8 random hex chars) and echoes it back in the `X-Request-Id` response header. Every log line of that request carries it:

```
2026-10-11 02:00:13.056  INFO [b780f3de] c.e.s.controller.NotificationController  : GET /api/v1/notifications -> … 200 in 22ms user=admin@travelmate.vn
```

Requests rejected before reaching a controller (401/403 from the security filters, 404) are logged by the filter as a WARN line.

## Masking
`LogSanitizer` decides how arguments and results appear: values whose name looks secret (`password`, `token`, `secret`, `otp`, `card`, `cvv`, `accountNumber`, `authorization`, `credential`) become `***`; DTOs are printed as JSON maps with those fields masked; JPA entities only as their class name (avoids lazy loading and leaking personal data); uploads as name + size; everything is cut at 300 characters.

## Where logs go

- **Console** (`docker compose logs backend`).
- **Daily files**: `logs/backend-YYYY-MM-DD.log` under the backend folder (in Docker `/app/logs`, mounted to `./logs`). Each day gets its own file, named and timestamped in Vietnam time; files older than 30 days, or beyond 1 GB in total, are deleted. Set `LOG_PATH` to use another folder. Files written from Docker are owned by root on the host (readable by everyone).

Defined in `src/main/resources/logback-spring.xml`.

## Configuration (`application.yml`)

```yaml
logging:
  file:
    path: ${LOG_PATH:logs}
  level:
    com.example.smart_booking_system: INFO
    com.example.smart_booking_system.service: ${LOG_LEVEL_SERVICE:INFO}   # DEBUG = trace every service call
app:
  logging:
    slow-threshold-ms: ${LOG_SLOW_THRESHOLD_MS:1000}
```

Set `LOG_LEVEL_SERVICE=DEBUG` (and optionally `LOG_SLOW_THRESHOLD_MS`) in `.env`, then `docker compose up -d backend`.

## Why an aspect
- Consistent format and no forgotten log lines: every new controller or service gets them automatically.
- Timing and status in one place make slow endpoints easy to spot.
- Business code stays readable.

Limit: Spring AOP works through proxies, so a call from one method to another **inside the same class** is not intercepted, and neither are `private` methods.

Further reading: [Spring AOP](https://docs.spring.io/spring-framework/reference/core/aop.html), [SLF4J MDC](https://www.slf4j.org/manual.html#mdc).
