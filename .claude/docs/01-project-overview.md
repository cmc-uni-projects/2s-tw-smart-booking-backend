# Project Overview — Smart Booking System ("TravelMate")

A Vietnamese hotel / villa / homestay booking marketplace, built as a university team project (CMC University). Brand name in the UI and emails: **TravelMate**. Original GitHub repo: `bquang016/Smart_Booking_System_Backend` (339 backend commits, 489 frontend commits).

## Two repositories

| Repo | What it is | Size |
|---|---|---|
| `2s-tw-smart-booking-backend` | Spring Boot 3 REST API (Java 17) | ~15.4k lines Java, 22 controllers, 24 entities |
| `2s-tw-smart-booking-frontend` | React 18 SPA (Vite, JS, Tailwind) | ~526 files under `src/` |

They talk over HTTP/JSON at `/api/v1/**`, authenticated with a **JWT Bearer token** stored in the browser's `localStorage`.

## Three kinds of user (roles)

| Role | Who | What they do |
|---|---|---|
| `CUSTOMER` | Every registered user (default role on sign-up) | Search, book, pay, cancel, request refunds, review, chat with AI, earn loyalty points |
| `OWNER` | A customer whose **owner application** was approved by an admin | List properties & rooms, create promotions, check guests in/out, see dashboard |
| `ADMIN` | Platform staff | Approve owners & properties, suspend property/room/user, manage refunds, platform-wide promotions, audit logs |

A user can hold several roles at once (an owner is also a customer). Demo accounts come from the Liquibase demo dataset (see [09](./09-database-migrations.md)) on startup.

## Core capabilities

1. **Auth** — email/password registration with email verification, login → JWT, logout (token blacklist), forgot/reset/change password, Google & Facebook OAuth2.
2. **Become an owner** — customer submits ID card + business licence → admin approves → user gains `OWNER` role.
3. **Property onboarding** — owner submits a property (+ rooms, images, amenities, policies) → status `PENDING` → admin approves (`APPROVE`) or rejects.
4. **Search & explore** — keyword search, featured list, nearby-on-map (lat/lng radius), property detail with availability.
5. **Booking** — nightly pricing with weekend price, overlap check, 5-minute payment window, auto-cancel.
6. **Payment** — *simulated*: the "pay" endpoint marks the payment `APPROVED` (no real VNPay/MoMo integration; logos & QR are UI only).
7. **Promotions** — owner codes (per property) + admin codes (platform), stackable, usage limits, membership-rank gating.
8. **Cancellation & refund** — policy-based penalty → customer files a refund request with bank details → admin approves/rejects.
9. **Stay lifecycle** — check-in → check-out → loyalty points (1 point / 1,000 VND) → membership rank.
10. **Ratings** — post-stay review with images; admin can hide, owner/admin can pin.
11. **Notifications** — in-app inbox (DB table, polled by UI) + HTML emails (21 Thymeleaf templates).
12. **AI** — Gemini-powered chatbot that can also run a guided "find me a room in city X for N people" search; room price forecast (weekday/weekend calendar).
13. **Admin tools** — dashboards, user management (lock/suspend), system audit logs (property/room changes).

## Where to go next

- [02-tech-stack.md](./02-tech-stack.md) — every library and what it's used for
- [03-architecture.md](./03-architecture.md) — how a request travels through both repos
- [04-domain-model.md](./04-domain-model.md) — entities, relations, status enums
- [05-business-flows.md](./05-business-flows.md) — step-by-step flows with state diagrams
- [06-api-map.md](./06-api-map.md) — every endpoint grouped by feature
- [07-gotchas-and-risks.md](./07-gotchas-and-risks.md) — mismatches, bugs, security holes found while reading
