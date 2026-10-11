# Logging (Spring AOP)

Instead of `log.info` calls scattered through every method, one **aspect** (`logging/LoggingAspect.java`) wraps controllers, services and scheduled jobs and logs around them. Plain SLF4J (`@Slf4j` / `LoggerFactory`) is still used for business events (e.g. `Email 'email/checkin-reminder' sent to … (en)`).

## What gets logged

Inputs and outputs of every call, at INFO:

```
→ POST /api/v1/auth/login AuthController.login(request=LoginRequest{email="customer@travelmate.vn", password=***, twoFactorCode=null}) user=anonymous
  → AuthServiceImpl.login(request=LoginRequest{email="customer@travelmate.vn", password=***, twoFactorCode=null})
  ← AuthServiceImpl.login = LoginResponse{accessToken=***, …, user=UserResponse{userId="…", fullName="Lê Hoàng Nam", …}} (236ms)
← POST /api/v1/auth/login 200 in 242ms result=ResponseEntity[200] ApiResponse{success=true, message="Đăng nhập thành công", …}
```

| Layer (pointcut) | INFO | WARN | ERROR |
|---|---|---|---|
| Controllers (`@RestController`) | `→` request line: route (query secrets masked), handler, arguments, user · `←` response line: status, duration, returned value | status ≥ 400, slow requests, business errors (`AppException`, with message key and arguments) | unexpected exceptions (with arguments) |
| Services (`..service..`) | `→` entry with arguments · `←` exit with return value and duration · `✗` business rejection; indented by call depth | slow calls, unexpected exceptions (with arguments) | n/a |
| Jobs (`@Scheduled`) | start / finish | slow runs | failures with stack trace |

Methods annotated **`@QuietLogging`** log the same lines at DEBUG (failures and slow calls still at WARN/ERROR): the UI's unread-count polling, the every-minute jobs (`checkExpiredBookings`, `scanAndCancelExpiredBookings`, `autoExpirePromotions`) and the JWT filter's blacklist check. Put it on any new high-frequency method.

Hand-written log lines cover business events (`Email '…' sent to … (en)`, `Booking 14 payment confirmed`, `Auto-cancelled expired booking 12`) and failures of side effects, which name the booking/property/application and include the stack trace (`Failed to send booking notifications (booking 14): …`).

`RequestIdFilter` (first filter in the chain) puts a request id in the MDC (taken from an incoming `X-Request-Id` or 8 random hex chars) and echoes it back in the `X-Request-Id` response header. Every log line of that request carries it, so `grep 1f2e3d4c` shows one request's whole trace.

Requests rejected before reaching a controller (401/403 from the security filters, 404) are logged by the filter as a WARN line.

## How values are rendered (`LogSanitizer`)
- **Secrets are masked by name** at any depth (argument, field or map key matching `password`, `token`, `secret`, `otp`, `card`, `cvv`, `accountNumber`, `authorization`, `credential`, `apiKey`) → `***`; so are `token=`/`code=`/`password=` query parameters.
- **DTOs** and other project objects: field by field.
- **JPA entities**: their column values; relations only as `Entity#id`, collections as `(size)` or `(lazy)`. Logging never triggers lazy loading or extra SQL.
- **Collections / pages / maps**: size plus the first 5 elements (`ListN(7)[1, 2, 3, 4, 5, …+2]`, `Page[0/3, total=25] […]`); `ResponseEntity[201] body`; uploads as `File(name, size)`; request/response/binding objects are skipped.
- Strings are cut at 200 characters, a whole line's arguments/result at `app.logging.max-length` (default 2000).

Personal data (names, emails, phones) does appear at INFO. Raise `LOG_LEVEL_SERVICE`/`LOG_LEVEL_APP` to `WARN` where that is not acceptable.

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
    com.example.smart_booking_system: ${LOG_LEVEL_APP:INFO}              # controllers + jobs
    com.example.smart_booking_system.service: ${LOG_LEVEL_SERVICE:INFO}  # WARN = only slow calls / failures
app:
  logging:
    slow-threshold-ms: ${LOG_SLOW_THRESHOLD_MS:1000}
    max-length: ${LOG_MAX_LENGTH:2000}
```

Set the variables in `.env` (Docker Compose) or `backend.logging.*` (Helm), then restart the backend. `DEBUG` additionally shows the `@QuietLogging` calls.

## Why an aspect
- Consistent format and no forgotten log lines: every new controller or service gets them automatically.
- Timing and status in one place make slow endpoints easy to spot.
- Business code stays readable.

Limit: Spring AOP works through proxies, so a call from one method to another **inside the same class** is not intercepted, and neither are `private` methods.

Further reading: [Spring AOP](https://docs.spring.io/spring-framework/reference/core/aop.html), [SLF4J MDC](https://www.slf4j.org/manual.html#mdc).
