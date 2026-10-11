# Code Map — "where is X?"

BE = `2s-tw-smart-booking-backend/src/main/java/com/example/smart_booking_system/`
FE = `2s-tw-smart-booking-frontend/src/`

| Feature | Frontend (page → service) | Backend (controller → service → repo) |
|---|---|---|
| Login / register | `pages/Auth/Login.jsx`, `Register.jsx` → `services/auth.service.js`; state in `context/AuthContext.jsx` | `AuthController` → `impl/AuthServiceImpl` → `UserRepository`, `RoleRepository` |
| JWT attach / 401 handling | `services/axios.config.js` (interceptors) | `security/JwtAuthenticationFilter`, `JwtTokenProvider`, `config/SecurityConfig` |
| Social login | `pages/Auth/OAuth2RedirectHandler.jsx` | `security/oauth2/*` |
| Role-gated pages | `routes/ProtectedRoute.jsx`, `routes/*Routes.jsx` | `SecurityConfig` URL rules + `@PreAuthorize` |
| Become owner | `pages/Partner/*` | `OwnerApplicationController`, `AdminApplicationController` → `impl/OwnerApplicationServiceImpl` |
| Property CRUD / submit | `pages/Owner/Properties/*` → `property.service.js` | `PropertyController` → `impl/PropertyServiceImpl` |
| Admin moderation | `pages/Admin/Hotels/*` → `admin.service.js` | `AdminPropertyController` |
| Rooms | `pages/Owner/Rooms/*` → `room.service.js` | `RoomController` → `impl/RoomServiceImpl` |
| Search / explore map | `pages/Customer/Home/*`, `pages/Customer/Explore/ExploreMapPage.jsx` | `PropertyController.search / nearby / featured` |
| Book | `pages/Customer/Booking/BookingPage.jsx`, `BookingForm.jsx` → `booking.service.js` | `BookingController.createBooking` → `BookingService.createBooking` |
| Promo codes on booking | `pages/Customer/Booking/components/DiscountSection.jsx` | `BookingService.applyPromotion / removePromotion` |
| Pay (simulated) | `pages/Customer/Booking/PaymentPage.jsx` → `payment.service.js` | `PaymentController` → `PaymentService.submitPayment` |
| Auto-cancel | `PaymentTimeoutModal.jsx` (UI timer) | `scheduler/BookingScheduler` → `BookingService.scanAndCancelExpiredBookings` |
| Cancel / refund | `pages/Customer/Booking/BookingHistory.jsx`; admin `pages/Admin/Transactions/RefundManagementPage.jsx` | `BookingService.cancelBooking`; `PaymentService.requestRefundByUser / processRefund` |
| Check-in / out | `pages/Owner/Bookings/*` | `BookingService.checkInBooking / checkOutBooking` |
| Reviews | → `rating.service.js`; admin `pages/Admin/Reviews/*` | `RatingController` → `RatingService` |
| Promotions | `pages/Owner/Promotions/*`, `pages/Admin/Promotions/*` → `promotion.service.js` | `PromotionController` → `PromotionService`; `scheduler/PromotionScheduler` |
| Notifications | `layouts/*/…NotificationDropdown.jsx` (polling) → `notification.service.js` | `NotificationController` → `NotificationService` |
| Emails | — | `impl/EmailServiceImpl` + `resources/templates/email/*.html` |
| AI chatbot | `layouts/CustomerLayout/ChatBot/*` → `ai.service.js` | `AiController` → `service/ai/AiService` |
| Price forecast | `components/ai/PriceForecast/*` | `RoomController.getPriceForecast` → `RoomServiceImpl` |
| Audit logs | `pages/Admin/AuditLogs/*` → `systemLog.service.js` | `SystemLogController` → `SystemLogService` |
| File upload | `components/common/Input/FileUpload.jsx` | `FileUploadController` → `FileStorageService` → R2 |
| Seed data | — | `config/DataInitializer` (roles, 3 dev users, amenities) |
