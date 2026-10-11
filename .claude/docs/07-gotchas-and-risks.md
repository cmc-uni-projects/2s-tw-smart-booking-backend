# Gotchas, Bugs & Risks

Things noticed while reading the code (2026-10-10). Each one is a good **learning exercise**: find it, understand why it's a problem, and fix it.

## 🔌 Local setup mismatches (fix these first or nothing connects)

1. **Port mismatch.** Backend defaults to `8080` (`server.port=${SERVER_PORT:8080}`, Dockerfile too). Frontend expects `8386` (axios fallback `http://localhost:8386/api/v1`, Vite proxy target `localhost:8386`).
   → Run backend with `SERVER_PORT=8386`, or change the frontend.
2. **Env var name mismatch.** FE `.env` defines `VITE_API_BASE_URL`, but `services/axios.config.js` reads `VITE_REACT_APP_API_URL` → always falls back to localhost. Some page components read `VITE_API_BASE_URL` (for image URLs), one reads `VITE_REACT_APP_API_URL`. Pick one name.
3. **Frontend calls an endpoint that doesn't exist:** `booking.service.js` → `GET /bookings/admin/refund-requests`. No such mapping in `BookingController`.
4. ~~`DataInitializer` seeds three accounts with well-known passwords on every startup~~ — **fixed 2026-10-11**: removed; demo accounts only via Liquibase context `demo`.

## 🔐 Security

5. **No ownership checks (IDOR).** Any logged-in user can:
   - read anyone's bookings: `GET /bookings/user/{userId}`, `GET /bookings/{id}`, `GET /bookings/property/{id}`;
   - cancel anyone's booking: `PUT /bookings/cancel/{id}`;
   - check out any booking: `PUT /bookings/checkout/{id}` (→ awards loyalty points);
   - pay / request refund on any booking.
6. **`createBooking` trusts `userId` from the request body** instead of the JWT principal → book on behalf of anyone.
7. **Payment is a single unauthenticated-in-spirit call.** `POST /payments/{id}/pay` marks it paid; no gateway, no signature verification.
8. `/api/v1/files/**` is `permitAll` → anonymous uploads to R2.
9. `/system-logs/**` has no role restriction → any user can read the audit trail.
10. Dead URL rule: `SecurityConfig` protects `/api/v1/applications/owner/**`, but the real path is `/applications/submit-owner`. It still works only because the controller has a class-level `@PreAuthorize("hasRole('CUSTOMER')")`.
11. JWT in `localStorage` (XSS-readable), 24h, no refresh, no rotation.
12. Error responses leak internals (`"Create booking failed: " + ex.getMessage()`).

## 💰 Business-logic bugs

13. **`Room.roomAmount` is ignored.** The overlap check is per `roomId` (= room *type*). A hotel with 10 "Deluxe" rooms can only take one Deluxe booking per night.
14. **Race condition on double booking.** Overlap check then insert, no lock / unique constraint. Two concurrent requests can both pass.
15. **Cancel allowed after check-in/completion.** `cancelBooking` only rejects `CANCELLED`; a `COMPLETED` stay can be "cancelled" for a refund calculation.
16. Pending bookings **block the room** (`PENDING_PAYMENT` counts in overlap) — intended to hold the room for 5 min, but combined with #5 anyone can lock rooms.
17. Scheduler window = 5 min + up to 60 s; FE timer = exactly 5 min. Minor.
18. `daysUntilCheckIn ≤ 1` → 100 % penalty even if free-cancellation policy says otherwise — order of rules matters.
19. Loyalty points are computed from `totalPrice` *after* discounts — a product decision worth confirming.
20. `Property.rating`/`reviewCount` denormalised → can drift if a rating is hidden/deleted.

## 🧱 Code-quality / architecture

21. ~~Services throw `RuntimeException` everywhere~~ — **mostly fixed 2026-10-11**: business errors are `AppException` subclasses carrying an i18n key + HTTP status, translated by `GlobalExceptionHandler` (response has `message` + stable `code`). Many controllers still `try/catch` and answer 400 themselves (now via `messages.of(e)`).
22. Mixed response shapes (raw DTO / `ApiResponse` / plain string).
23. ~~`ddl-auto=update` in production~~ — **fixed 2026-10-11**: Liquibase owns the schema, Hibernate validates.
24. Unused dependencies: Redis, WebSocket, Quartz; FE: Redux (empty files), 3 date libs, 3 map libs.
25. Empty CI/CD workflow files; tests: the auto-generated context test plus i18n tests (`EmailTemplatesTest`, `MessageBundlesTest`).
26. AI chat session state in a JVM `HashMap` — lost on restart, breaks with >1 instance.
27. ~~`System.out.println` debug logging mixed with SLF4J~~ — **fixed 2026-10-11**: SLF4J only, plus the `LoggingAspect` (see [11](./11-logging.md)).
28. Enum naming inconsistencies: `PropertyStatus.APPROVE` vs `ApplicationStatus.APPROVED`; `AuthProvider.local` lower-case.
29. `Rating.ratingType` lacks `@Enumerated(STRING)` → stored as ordinal; reordering the enum corrupts data.

## Found while dockerising (2026-10-10)

30. **`GET /auth/me` returns the BCrypt `password` hash** of the user (it serialises the entity / UserDetails). Never send credentials to the client.
31. ~~**`findFeaturedProperties` native SQL uses snake_case**~~ **fixed 2026-10-11**. (`is_active`, `property_status`) while the naming strategy is `PhysicalNamingStrategyStandardImpl` (camelCase columns). It fails on a fresh DB (500 on `/properties/featured`). It probably only works in prod because that schema was created under the old naming. The other native queries use camelCase.
32. **Two S3 clients configured differently:** `R2Config` sets `pathStyleAccessEnabled(true)`, `FileStorageService` builds its own client without it (virtual-hosted). One bean should be shared.
33. **Frontend hardcodes the team's public R2 bucket** (`https://pub-…r2.dev/`) in `utils/image.util.js` for keys without `http`. Local/other environments can't serve those images.
34. `Dotenv.load()` throws when `.env` is absent, so the app can't start from env vars alone (hostile to containers/CI). `Dotenv.configure().ignoreIfMissing().load()` fixes it.
35. **Register form vs backend password rules disagree.** FE (`Register.jsx`): ≥ 6 chars, upper, digit, special from `!@#$%^&*(),.?":{}|<>`, and the button enables at strength ≥ 2. BE (`AuthServiceImpl.isStrongPassword` + DTO `@Size(min=8)`): ≥ 8 chars, upper **and lower**, digit, special from `@#$%^&+=!.*`. So `Abc@123` or `Abcdef1(` pass the form but get a 400.
36. Verification token lives only **10 minutes** (`plusMinutes(10)`); an expired link leaves the user stuck unless they find "resend verification".
37. ~~Guests couldn't open a hotel page~~ — **fixed 2026-10-11**: `GET /properties/{id}/policies` (and rooms/ratings/forecast/availability/promotions reads) required login → 401 → "Couldn't load". Now public GETs in `SecurityConfig`.
38. `HotelHeader` always shows the badge "Hotel"/"Khách sạn" regardless of `propertyType` (resorts, villas…).
38. **Matching on message text breaks with translation.** Backend messages now follow `Accept-Language`, so frontend code must branch on the response `code` (e.g. `error.promotion.codeConflict`, `auth.lockedOrSuspended`), never on `message.includes("…")`.
39. ~~Real emails in local dev~~ — **fixed 2026-10-11**: the default `local` profile always sends to Mailpit; real SMTP only with `SPRING_PROFILES_ACTIVE=dev`.
40. ~~Every new booking "expired" instantly~~ — **fixed 2026-10-11**: the JVM ran in UTC (Docker default) while the DB/Hibernate use Vietnam time, so the API returned `createdAt` 7 h behind; the payment page read it as local time, saw > 5 min elapsed and cancelled the booking. Now `SmartBookingSystemApplication` sets the default zone to `Asia/Ho_Chi_Minh`, and the frontend parses server timestamps with `utils/serverTime.js` (`parseServerTime`, adds `+07:00`). Use it whenever you compare a server time with `Date.now()`.
