# Lesson 0001 — The Big Picture: one click, two repos

**Time:** ~20 min · **Win:** you can trace a real request from a React button to a MySQL row and name every file on the way.

← [Index](../README.md) · Reference: [Code map](../reference/code-map.md) · [Glossary](../reference/glossary.md) · Next: [0002 Run it on Kubernetes](./0002-run-it-on-kubernetes.md)

---

## 1. The shape of the system (2 min)

There are two programs:

- **Frontend**: a React app in the browser. It has no database. It only *asks*.
- **Backend**: a Spring Boot app. It owns the data (MySQL), the rules, and the side effects (email, files, AI).

They talk in JSON over HTTP at `/api/v1/**`. Every request after login carries **`Authorization: Bearer <JWT>`** ([glossary](../reference/glossary.md)).

Three roles drive everything: `CUSTOMER` books, `OWNER` lists properties, `ADMIN` approves and moderates. → [01-project-overview](../docs/01-project-overview.md)

## 2. The layers: the one pattern to remember (5 min)

Both sides are layered, and every feature uses the same layers:

```
FRONTEND                               BACKEND
page component (.jsx)                  controller/  ← HTTP in/out, no business rules
   ↓ calls                                ↓
services/xxx.service.js                service/     ← the rules (@Transactional)
   ↓ uses                                 ↓
services/axios.config.js  ──HTTP──▶    repository/  ← Spring Data JPA → SQL
   (adds the token)                       ↓
                                       entity/      ← Java class = DB table
```

Before the controller runs, every request goes through **Spring Security's filter chain**. Our custom `JwtAuthenticationFilter` reads the token, checks it hasn't been blacklisted, loads the user, and puts the user and their `ROLE_*` authorities into the `SecurityContext`. Then `SecurityConfig`'s URL rules and the `@PreAuthorize` annotations decide whether to let the request through. ([Spring Security architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html))

## 3. Worked example: "Cancel booking" (5 min, read with the code open)

| # | Where | What happens |
|---|---|---|
| 1 | FE `pages/Customer/Booking/BookingHistory.jsx` | user clicks Cancel |
| 2 | FE `services/booking.service.js` | `api.put('/bookings/cancel/' + id)` |
| 3 | FE `services/axios.config.js` | request interceptor adds `Bearer` from `localStorage.accessToken` ([axios interceptors](https://axios-http.com/docs/interceptors)) |
| 4 | BE `security/JwtAuthenticationFilter.java` | validates token → loads user → sets SecurityContext |
| 5 | BE `config/SecurityConfig.java` | no specific rule, so `anyRequest().authenticated()` applies ✓ |
| 6 | BE `controller/BookingController.java` | `@PutMapping("/cancel/{bookingId}")` → `bookingService.cancelBooking(id)` |
| 7 | BE `service/BookingService.java` | `cancelBooking`: unpaid → cancel free; paid → penalty 0 / 30 / 100 % by policy |
| 8 | BE `repository/BookingRepository`, `PaymentRepository` | `findById`, `save` → SQL on `bookings`, `payment` |
| 9 | BE `NotificationService`, `EmailService` | inbox rows + HTML email (failures swallowed) |
| 10 | back up the chain | JSON `BookingResponseDTO` → page re-renders |

Notice what **didn't** happen at step 5 or 6: nobody checked that booking `id` belongs to the caller. Keep that in mind; [gotcha #5](../docs/07-gotchas-and-risks.md) covers it.

## 4. Your turn: trace one yourself (5 min) 🛠️

Trace **"Customer pays for a booking"** through the same 10 steps. Open the files and fill in the blanks (on paper or in a reply to me):

1. FE page: `pages/Customer/Booking/__________.jsx`
2. FE service function + HTTP method + path: `__________`
3. BE controller + method: `__________`
4. BE service method: `__________`
5. Which **two** rows change status, and to what?
6. What would make this whole transaction roll back? (hint: promotions)

<details><summary>Check your answers</summary>

1. `PaymentPage.jsx`
2. `payment.service.js` → `api.post('/payments/{bookingId}/pay', null, {params: …})`
3. `PaymentController.submitPayment` (`@PostMapping("/{bookingId}/pay")`, query params `method`, `note`)
4. `PaymentService.submitPayment`
5. `Payment.paymentStatus` `PENDING → APPROVED`; `Booking.status` `PENDING_PAYMENT → CONFIRMED`
6. A promotion code's `usageLimit` is already reached: `incrementUsageCountIfAvailable` returns 0, the service throws, and `@Transactional` rolls everything back ([@Transactional docs](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)). Also: there's no real payment gateway; this call *is* the payment.
</details>

## 5. Retrieval quiz (3 min): answer before opening

**Q1.** Where is the JWT attached to outgoing requests?
- A) inside every page component file
- B) in the axios request interceptor
- C) inside the React Router config
- D) inside the Vite proxy setting

**Q2.** A request reaches `BookingController`. What has *already* happened?
- A) the service saved the booking
- B) the filter authenticated the user
- C) the repository opened a transaction
- D) the email was already sent

**Q3.** Which role does a brand-new registered user get?
- A) OWNER, after their email is verified
- B) CUSTOMER, assigned during the registration
- C) ADMIN, only for the first account
- D) No role until an admin approves

**Q4.** `ProtectedRoute.jsx` hides the admin pages from customers. Is the admin API therefore safe?
- A) Yes, the hidden route blocks calls
- B) No, the backend must check roles
- C) Yes, axios refuses admin URLs then
- D) No, because React Router is broken

<details><summary>Answers</summary>

- **Q1 B.** `services/axios.config.js`, request interceptor, reading `localStorage`.
- **Q2 B.** Filters run before any controller. Transactions start in `@Transactional` services, *after* the controller is called.
- **Q3 B.** `AuthServiceImpl.register` adds role `CUSTOMER`. `OWNER` comes only from an approved owner application.
- **Q4 B.** Anyone can call the API with curl. The real gate is `SecurityConfig` (`/api/v1/admin/**` → `hasRole("ADMIN")`) plus `@PreAuthorize`.
</details>

## 📖 Primary source for this lesson
**[Spring Security: Servlet Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)**: read the sections *DelegatingFilterProxy → SecurityFilterChain → SecurityContextHolder*. It explains steps 4–5 above better than anything else.

## 💬 Ask me
Anything unclear: why filters run before controllers, what `@Transactional` really does, why the code has both URL rules *and* `@PreAuthorize`? Ask me. I'm your teacher for this codebase. Also tell me how the quiz went, so I can pitch lesson 0002 at the right level.
