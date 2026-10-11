# API Map

Base: `/api/v1`. Auth column: **P** = public, **A** = any logged-in user, or role(s). Taken from `@*Mapping` + `@PreAuthorize` + `SecurityConfig`. Frontend caller = file in `2s-tw-smart-booking-frontend/src/services/`.

Response shape is **not uniform**: some endpoints return `ApiResponse{success,message,data,timestamp,path}`, some return the DTO directly, some return a plain string on error.

## Auth — `AuthController` → `auth.service.js`
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/auth/register` | P | sign up (role CUSTOMER, sends verify email) |
| POST | `/auth/login` | P | email+password → `{accessToken, user}` |
| POST | `/auth/logout` | P* | blacklist current token |
| GET | `/auth/verify-email?token=` | P | confirm email |
| POST | `/auth/resend-verification` | P | |
| POST | `/auth/forgot-password` · `/auth/reset-password` | P | |
| POST | `/auth/change-password` | P* | needs token in practice |
| GET | `/auth/me` | P* | current user |
| POST | `/auth/create-password` · DELETE `/auth/social/unlink` | P* | social accounts |

\* under `/auth/**` which is `permitAll`; the method reads the principal itself.

OAuth2 (not under /api): `GET /oauth2/authorization/{google|facebook}` → callback `/login/oauth2/code/{provider}`.

## Users & profile
| Method | Path | Auth |
|---|---|---|
| GET | `/user-details/me` · PUT `/user-details/update` · GET `/user-details/profile-status` · POST `/user-details/upload-avatar` | A |
| GET | `/admin/users` · `/admin/users/{userId}` · PATCH `/admin/users/{userId}/status` | ADMIN |
| GET | `/searchhistory/search` | CUSTOMER/OWNER/ADMIN |

## Owner applications
| Method | Path | Auth |
|---|---|---|
| POST | `/applications/submit-owner` (multipart) | CUSTOMER |
| GET | `/admin/owner-applications` · POST `/admin/owner-applications/{id}/review` | ADMIN |
| GET | `/admin/owner-applications/dashboard-stats` · `/dashboard/owners` · `/dashboard/cities` | ADMIN (admin dashboard lives here) |

## Properties — `PropertyController`, `AdminPropertyController` → `property.service.js`, `admin.service.js`
| Method | Path | Auth |
|---|---|---|
| GET | `/properties/search` · `/properties/featured` · `/properties/{id}` | P |
| GET | `/properties/nearby?lat&lng&radius` · `/properties/check-name` | A |
| POST | `/properties/submit-application` | OWNER |
| POST | `/properties/add` · PUT `/properties/update/{id}` | OWNER/ADMIN |
| GET | `/properties/my-properties` · `/properties/my-active-properties` | OWNER |
| PATCH | `/properties/{id}/status` (toggle isActive) | OWNER |
| POST/PUT/GET | `/properties/{id}/policies` | A |
| GET | `/property-details/{propertyId}` | A |
| POST/GET/DELETE/PUT | `/property-images/{propertyId}/upload` · `/{propertyId}` · `/{propertyId}/{imageId}` · `/{propertyId}/{imageId}/cover` | A |
| POST/GET/PUT/DELETE | `/propertyAmenity/...` | OWNER |
| GET | `/admin/properties/status?status=` · `/admin/properties/active` · `/admin/properties/list` | ADMIN |
| POST | `/admin/properties/{id}/review` | ADMIN |
| PUT | `/admin/properties/{id}/suspend` · `/{id}/activate` · `/rooms/{roomId}/suspend` · `/rooms/{roomId}/activate` | ADMIN |
| GET | `/admin/properties/{id}/rooms` | ADMIN |

## Rooms — `RoomController` → `room.service.js`
| Method | Path | Auth |
|---|---|---|
| GET | `/rooms/property/{propertyId}` · `/rooms/{roomId}` · `/rooms/check-name` | A |
| POST | `/rooms/add` (multipart) · PUT `/rooms/{roomId}` · DELETE `/rooms/{roomId}` | OWNER |
| GET | `/rooms/{roomId}/price-forecast?startDate&days` | A |
| GET | `/room-details/{roomId}` | A |
| … | `/room-images/...`, `/roomAmenity/...` | A / OWNER |

## Amenities
`GET /amenity/all`, `GET /amenity/type/{ROOM|PROPERTY}` (A); `POST /amenity/add`, `PUT /amenity/update/{id}`, `DELETE /amenity/delete/{id}` (ADMIN).

## Bookings — `BookingController` → `booking.service.js`
| Method | Path | Auth |
|---|---|---|
| POST | `/bookings/create` | A |
| GET | `/bookings/{bookingId}` · `/bookings/user/{userId}` · `/bookings/property/{propertyId}` | A (no ownership check) |
| GET | `/bookings` | ADMIN |
| GET | `/bookings/room/{roomId}/availability` | A |
| PUT | `/bookings/{id}/apply-promotion?code=` | CUSTOMER |
| PUT | `/bookings/{id}/remove-promotion?code=` | CUSTOMER/ADMIN |
| PUT | `/bookings/cancel/{bookingId}` | A |
| PUT | `/bookings/{id}/check-in` | OWNER/ADMIN |
| PUT | `/bookings/checkout/{bookingId}` | A |
| PUT | `/bookings/approve-refund/{bookingId}` | ADMIN (legacy) |

## Payments & refunds — `PaymentController` → `payment.service.js`
| Method | Path | Auth |
|---|---|---|
| POST | `/payments/{bookingId}/pay?method=&note=` | A |
| GET | `/payments/my-history` | A |
| POST | `/payments/{bookingId}/refund-request` | A |
| PUT | `/payments/refund-process/{requestId}?approve=&note=` | ADMIN |
| GET | `/payments/all` | ADMIN |

## Promotions — `PromotionController` → `promotion.service.js`
`POST /promotions/create`, `PUT /update/{id}`, `DELETE /delete/{id}`, `PUT /toggle/{id}`, `GET /property/{propertyId}`, `POST /{id}/banner` (OWNER/ADMIN) · `GET /all`, `/detail/{id}`, `/suggest` (A) · `GET /owner/my-promotions` (OWNER).

## Ratings — `RatingController` → `rating.service.js`
`POST /rating/create`, `PUT /rating/update/{id}` (multipart) · `GET /rating/booking/{id}`, `/property/{id}`, `/{id}` · `DELETE /rating/delete/{id}` (ADMIN/CUSTOMER/OWNER) · `PATCH /rating/hide/{id}` (ADMIN) · `PATCH /rating/pin/{id}` (ADMIN/OWNER).

## Other
| Area | Endpoints | Auth |
|---|---|---|
| Owner dashboard | `GET /owner/dashboard/stats` | OWNER |
| Notifications | `GET /notifications`, `/unread-count`, `PUT /{id}/read`, `/read-all` | A |
| AI | `POST /ai/chat {message}` | A |
| Files | `POST /files/upload` (→ R2) | P (path is permitAll!) |
| Audit logs | `GET /system-logs`, `/entity`, `/actor/{userId}`, `/type/{entityType}`, `/{logId}` | A (no role check) |
