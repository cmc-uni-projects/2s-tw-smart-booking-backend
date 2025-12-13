package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final PropertyPoliciesRepository policiesRepo;
    private final EmailService emailService;
    private final PaymentRepository paymentRepo;
    private final PromotionRepository promotionRepo;
    private final RatingRepository ratingRepository;
    private final RefundRequestRepository refundRepo;
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // ================================
    // CREATE BOOKING (chặt chẽ, capacity cho mọi loại)
    // ================================
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO req) {

        // --- 1) Kiểm tra tồn tại cơ bản (Giữ nguyên) ---
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));

        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found: " + req.getPropertyId()));

        // Logic chọn Room (Giữ nguyên)
        boolean requiresWholeRoom =
                property.getPropertyType() == PropertyType.VILLA ||
                        property.getPropertyType() == PropertyType.HOMESTAY;

        Room room;
        if (requiresWholeRoom) {
            if (req.getRoomId() != null) {
                throw new RuntimeException("Do not send roomId for Villa/Homestay. Booking is always the WHOLE room.");
            }
            room = roomRepo.findByPropertyIdAndCategory(req.getPropertyId(), RoomCategory.WHOLE)
                    .orElseThrow(() -> new RuntimeException("Whole room not found for property " + req.getPropertyId()));
        } else {
            if (req.getRoomId() == null) {
                throw new RuntimeException("roomId is required for HOTEL/RESORT booking");
            }
            room = roomRepo.findById(req.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));

            if (room.getPropertyId() == null || room.getPropertyId().getPropertyId() != req.getPropertyId()) {
                throw new RuntimeException("Room does not belong to the given property");
            }
        }

        // --- 3) Validate Guest (Giữ nguyên) ---
        if (req.getGuestCount() == null || req.getGuestCount() <= 0) {
            throw new RuntimeException("guestCount must be greater than 0");
        }
        if (req.getGuestCount() > room.getCapacity()) {
            throw new RuntimeException("guestCount exceeds room capacity");
        }

        // --- 4) Validate Date (Giữ nguyên) ---
        if (req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new RuntimeException("Dates are required");
        }
        if (!req.getCheckInDate().isBefore(req.getCheckOutDate())) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }

        // --- 5) Check Overlapping (Giữ nguyên) ---
        List<Booking> overlapping = bookingRepo.findConfirmedOverlappingByRoomId(
                room.getRoomId(), req.getCheckInDate(), req.getCheckOutDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Room is already booked in the selected dates");
        }

        // --- 6) Tính tiền (Giữ nguyên) ---
        long nights = ChronoUnit.DAYS.between(req.getCheckInDate(), req.getCheckOutDate());
        if (nights <= 0) nights = 1;
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        // --- 7) TẠO BOOKING & LƯU THÔNG TIN KHÁCH ---
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setProperty(property);
        booking.setRoom(room);
        booking.setCheckInDate(req.getCheckInDate());
        booking.setCheckOutDate(req.getCheckOutDate());
        booking.setGuestCount(req.getGuestCount());
        booking.setTotalPrice(total);
        booking.setPenaltyAmount(BigDecimal.ZERO);
        booking.setRefundAmount(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        // Xử lý thông tin người ở
        if (req.isBookingForSelf()) {
            booking.setCustomerName(user.getFullName());
            booking.setCustomerPhone(user.getPhoneNumber());
            booking.setCustomerEmail(user.getEmail());
        } else {
            if (req.getContactName() == null || req.getContactPhone() == null) {
                throw new RuntimeException("Contact info is required when booking for others");
            }
            booking.setCustomerName(req.getContactName());
            booking.setCustomerPhone(req.getContactPhone());
            booking.setCustomerEmail(req.getContactEmail());
        }
        booking.setSpecialRequest(req.getSpecialRequest());

        bookingRepo.save(booking);

        // --- 8) Tạo Payment PENDING (Giữ nguyên) ---
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTotalAmount(total);
        payment.setPaymentMethod(null);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        // --- 9) Logic Gửi Email (Giữ nguyên) ---
        try {
            emailService.sendPaymentReminderEmail(
                    booking.getCustomerEmail(),
                    booking.getCustomerName(),
                    String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice().toString()
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }

        // ========================================================================
        // 🔥 [CẬP NHẬT] GỬI THÔNG BÁO (NOTIFICATION)
        // ========================================================================
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String propertyName = property.getPropertyName();

            // 1. Gửi cho OWNER (Người nhận được đơn)
            User owner = property.getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(), // [SỬA] Truyền String ID
                        "Đơn đặt phòng mới #" + booking.getBookingId(),
                        "Khách hàng " + booking.getCustomerName() + " vừa đặt phòng tại " + propertyName + ". Trạng thái: Chờ thanh toán.",
                        NotificationType.BOOKING_RECEIVED, // [SỬA] Dùng Type của Owner
                        relatedId
                );
            }

            // 2. Gửi cho CUSTOMER (Người đặt) - Để họ biết đã tạo đơn thành công và cần thanh toán
            notificationService.sendNotification(
                    user.getUserId(), // [SỬA] Truyền String ID
                    "Đặt phòng thành công - Chờ thanh toán",
                    "Bạn vừa đặt phòng tại " + propertyName + ". Vui lòng hoàn tất thanh toán để giữ phòng.",
                    NotificationType.BOOKING_SUCCESS, // [SỬA] Dùng Type của Customer (Tạm coi là success bước giữ chỗ)
                    relatedId
            );

        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo (không ảnh hưởng booking): " + e.getMessage());
        }

        return convertToDTO(booking);
    }

    // ================================
    // CANCEL BOOKING
    // ================================
    @Transactional
    public BookingResponseDTO cancelBooking(int bookingId) {
        // 1. Kiểm tra tồn tại
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        String relatedId = String.valueOf(booking.getBookingId());

        // --- TRƯỜNG HỢP 1: CHƯA THANH TOÁN (Hủy ngay) ---
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPenaltyAmount(BigDecimal.ZERO);
            booking.setRefundAmount(BigDecimal.ZERO);

            Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.REJECTED);
                paymentRepo.save(payment);
            }

            bookingRepo.save(booking);

            // -> Thông báo cho KHÁCH: Hủy thành công
            notificationService.sendNotification(
                    booking.getUser().getUserId(), // String ID
                    "Hủy đặt phòng thành công",
                    "Đơn đặt phòng #" + booking.getBookingId() + " đã được hủy thành công. Bạn không bị tính phí.",
                    NotificationType.BOOKING_CANCELLED,
                    relatedId
            );

            return convertToDTO(booking);
        }

        // --- TRƯỜNG HỢP 2: ĐÃ THANH TOÁN (Tính toán hoàn tiền) ---
        PropertyPolicies policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = booking.getCheckInDate();

        BigDecimal refundAmount;
        BigDecimal penaltyAmount;

        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, checkInDate);

        // Logic tính phí phạt (Giữ nguyên của bạn)
        if (daysUntilCheckIn <= 1) {
            penaltyAmount = booking.getTotalPrice();
            refundAmount = BigDecimal.ZERO;
        } else {
            boolean isFreeCancellation = false;
            if (policies != null && Boolean.TRUE.equals(policies.isAllowFreeCancellation())) {
                Integer freeDays = policies.getFreeCancellationDays();
                if (freeDays == null) freeDays = 0;
                LocalDate freeDeadline = checkInDate.minusDays(freeDays);

                if (!today.isAfter(freeDeadline)) {
                    isFreeCancellation = true;
                }
            }

            if (isFreeCancellation) {
                penaltyAmount = BigDecimal.ZERO;
                refundAmount = booking.getTotalPrice();
            } else {
                penaltyAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.30)); // Phí 30%
                refundAmount = booking.getTotalPrice().subtract(penaltyAmount);
            }
        }

        // Cập nhật Booking
        booking.setPenaltyAmount(penaltyAmount);
        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);

        // 4. TỰ ĐỘNG TẠO YÊU CẦU HOÀN TIỀN
        /*if (refundAmount.compareTo(BigDecimal.ZERO) > 0 && payment != null && payment.getPaymentStatus() == PaymentStatus.APPROVED) {
            if (!refundRepo.existsByBooking(booking)) {
                RefundRequest refund = new RefundRequest();
                refund.setBooking(booking);
                refund.setAmount(refundAmount);
                refund.setStatus(RefundRequestStatus.PENDING);
                refund.setReason("Khách hủy phòng (Hệ thống tự động tạo)");
                refund.setRequestDate(LocalDateTime.now());
                refundRepo.save(refund);

                payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);
                paymentRepo.save(payment);
                hasRefundRequest = true;
            }
        }
*/
        // 5. Gửi Email thông báo
        try {
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
            emailService.sendCancellationRequestReceivedEmail(
                    emailTo, nameTo, String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice(), penaltyAmount, refundAmount
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail cancel: " + e.getMessage());
        }

        // ========================================================================
        // 🔥 [CẬP NHẬT] GỬI THÔNG BÁO (NOTIFICATION)
        // ========================================================================
        try {
            // 1. Gửi cho CUSTOMER (Quan trọng nhất)
            String customerMsg;
            if (hasRefundRequest) {
                customerMsg = "Đơn #" + booking.getBookingId() + " đã hủy. Yêu cầu hoàn tiền "
                        + String.format("%,.0f", refundAmount) + " VNĐ đã được tạo và đang chờ Admin xử lý.";
            } else {
                customerMsg = "Đơn #" + booking.getBookingId() + " đã hủy. Rất tiếc, bạn không được hoàn tiền do quá hạn hủy miễn phí.";
            }

            notificationService.sendNotification(
                    booking.getUser().getUserId(), // String ID
                    "Đã hủy đặt phòng",
                    customerMsg,
                    NotificationType.BOOKING_CANCELLED, // Type dành cho Customer
                    relatedId
            );

            // 2. Gửi cho OWNER (Vẫn cần báo để họ biết phòng trống)
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(), // String ID
                        "Khách đã hủy phòng #" + booking.getBookingId(),
                        "Khách hàng đã hủy đơn đặt phòng. Lịch phòng đã được mở lại.",
                        NotificationType.BOOKING_CANCELLED_BY_GUEST, // Type dành cho Owner
                        relatedId
                );
            }

        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo: " + e.getMessage());
        }

        return convertToDTO(booking);
    }



    // ================================
    // HÀM MỚI: TỰ ĐỘNG QUÉT ĐƠN QUÁ HẠN (Scheduler sẽ gọi hàm này)
    // ================================
    @Transactional
    public void scanAndCancelExpiredBookings() {
        // Thời gian hiện tại trừ đi 5 phút
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);

        // Tìm các đơn PENDING_PAYMENT được tạo trước thời điểm expirationTime
        List<Booking> expiredBookings = bookingRepo.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT,
                expirationTime
        );

        if (!expiredBookings.isEmpty()) {
            logger.info("Tìm thấy {} đơn hàng quá hạn thanh toán (5 phút). Đang hủy...", expiredBookings.size());

            for (Booking booking : expiredBookings) {
                try {
                    // Gọi lại hàm cancelBooking ở trên để tái sử dụng logic
                    cancelBooking(booking.getBookingId());
                    logger.info("Đã tự động hủy đơn booking ID: {}", booking.getBookingId());
                } catch (Exception e) {
                    logger.error("Lỗi khi tự động hủy đơn ID {}: {}", booking.getBookingId(), e.getMessage());
                }
            }
        }
    }

    // ================================
    // APPROVE REFUND (Admin thực hiện)
    // ================================
    @Transactional
    public void approveRefund(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        RefundRequest refundRequest = refundRepo.findByBooking(booking).orElseThrow(() -> new RuntimeException("Refund Request not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new RuntimeException("Payment not found"));

        refundRequest.setStatus(RefundRequestStatus.APPROVED);
        refundRequest.setResolveDate(LocalDateTime.now());
        refundRequest.setAdminNote("Admin approved.");
        refundRepo.save(refundRequest);

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmount(refundRequest.getAmount());
        paymentRepo.save(payment);

        try {
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

            emailService.sendCancellationSuccessEmail(
                    emailTo, nameTo, String.valueOf(booking.getBookingId()),
                    String.format("%,.0f", booking.getRefundAmount()),
                    String.format("%,.0f", booking.getPenaltyAmount())
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail success refund: " + e.getMessage());
        }
    }

    // 3. CHECK-IN (Vận hành)
    @Transactional
    public void checkInBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ được Check-in đơn ĐÃ XÁC NHẬN");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepo.save(booking);
    }

    // 4. CHECK-OUT (Hoàn tất)
    @Transactional
    public void checkOutBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Cho phép checkout nếu đang ở hoặc đã confirm (quên checkin)
        if (booking.getStatus() != BookingStatus.CHECKED_IN && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Trạng thái không hợp lệ để Check-out");
        }

        // 1. Cập nhật trạng thái Booking
        booking.setStatus(BookingStatus.COMPLETED);

        // 2. TÍCH ĐIỂM & THĂNG HẠNG
        if (booking.getTotalPrice() != null) {
            User user = booking.getUser();

            // Tính điểm: 1000 VND = 1 điểm (Lấy phần nguyên)
            int earnedPoints = booking.getTotalPrice().divide(BigDecimal.valueOf(1000)).intValue();

            if (earnedPoints > 0) {
                // Cộng điểm vào tổng điểm hiện tại
                int currentPoints = user.getPoints(); // Đảm bảo User entity đã có field này (mặc định 0)
                int newTotalPoints = currentPoints + earnedPoints;
                user.setPoints(newTotalPoints);

                // Cập nhật hạng thành viên dựa trên điểm mới
                updateUserRank(user, newTotalPoints);

                // Lưu thông tin User mới
                userRepo.save(user);
            }
        }

        bookingRepo.save(booking);

        try {
            // Lấy email và tên người nhận (Ưu tiên thông tin Customer trong booking)
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
            String propertyName = booking.getProperty().getPropertyName();

            // Gọi service gửi email
            emailService.sendThankYouEmail(
                    emailTo,
                    nameTo,
                    String.valueOf(booking.getBookingId()),
                    propertyName
            );
            logger.info("✅ Đã gửi email cảm ơn checkout cho Booking ID: {}", bookingId);
        } catch (Exception e) {
            // Log lỗi nhưng không chặn transaction checkout (để khách vẫn checkout được dù lỗi mail)
            logger.error("❌ Lỗi gửi mail cảm ơn sau checkout: {}", e.getMessage());
        }
    }

    // ============================================================
    // 🔥 1. HÀM STATIC: TÍNH RANK TỪ ĐIỂM (DÙNG CHUNG TOÀN HỆ THỐNG)
    // ============================================================
    public static MembershipRank calculateRankFromPoints(int points) {
        if (points >= 10000) {
            return MembershipRank.DIAMOND;
        } else if (points >= 5000) {
            return MembershipRank.GOLD;
        } else if (points >= 1000) {
            return MembershipRank.SILVER;
        } else {
            return MembershipRank.BRONZE;
        }
    }

    // ============================================================
    // 🔥 2. HÀM UPDATE RANK CHO USER (SỬ DỤNG HÀM STATIC Ở TRÊN)
    // ============================================================
    public void updateUserRank(User user, int points) {
        // Tái sử dụng logic tính toán để tránh lặp code
        MembershipRank newRank = calculateRankFromPoints(points);
        user.setMembershipRank(newRank);
    }


    // ================================
    // GET APIs
    // ================================
    public BookingResponseDTO getBookingById(int bookingId) {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        return convertToDTO(b);
    }

    public List<BookingResponseDTO> getBookingsByUserId(String userId) {
        return bookingRepo.findByUserUserId(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingsByPropertyId(int propertyId) {
        return bookingRepo.findByPropertyPropertyId(propertyId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepo.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ================================
    // helper convert
    // ================================
    private BookingResponseDTO convertToDTO(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();

        // 1. Payment Info (Lấy từ bảng Payment)
        Payment payment = paymentRepo.findByBooking_BookingId(b.getBookingId()).orElse(null);
        if (payment != null) {
            dto.setPaymentStatus(payment.getPaymentStatus().name());
            // ✅ Map thêm Payment Method (Ví dụ: Momo, VNPay...)
            dto.setPaymentMethod(payment.getPaymentMethod());
        }

        // 2. Basic Info
        dto.setBookingId(b.getBookingId());
        dto.setPropertyId(b.getProperty().getPropertyId());
        dto.setRoomId(b.getRoom() != null ? b.getRoom().getRoomId() : null);

        // 3. Date & Money
        dto.setCheckInDate(b.getCheckInDate());
        dto.setCheckOutDate(b.getCheckOutDate());
        dto.setGuestCount(b.getGuestCount());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setPenaltyAmount(b.getPenaltyAmount());
        dto.setRefundAmount(b.getRefundAmount());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setAdminPromotionCode(b.getAdminPromotionCode());
        dto.setOwnerPromotionCode(b.getPromotionCode());
        dto.setReviewed(ratingRepository.existsByBookingId_BookingId(b.getBookingId()));
        dto.setDiscountAmount(b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO);
        dto.setPromotionCode(b.getPromotionCode());
        dto.setStatus(b.getStatus());

        // ✅ Map thêm Special Request
        dto.setSpecialRequest(b.getSpecialRequest());

        // 4. Map Property Info (Ảnh bìa, địa chỉ...)
        if (b.getProperty() != null) {
            dto.setPropertyName(b.getProperty().getPropertyName());
            dto.setPropertyAddress(b.getProperty().getAddress() + ", " + b.getProperty().getCity());

            String coverUrl = null;
            if (b.getProperty().getImages() != null && !b.getProperty().getImages().isEmpty()) {
                coverUrl = b.getProperty().getImages().stream()
                        .filter(PropertyImage::isCover)
                        .findFirst()
                        .map(PropertyImage::getImageUrl)
                        .orElse(null);

                if (coverUrl == null) {
                    coverUrl = b.getProperty().getImages().get(0).getImageUrl();
                }
            }
            dto.setPropertyImage(coverUrl);
        }

        // 5. Map Room Info
        if (b.getRoom() != null) {
            dto.setRoomName(b.getRoom().getRoomName());
        }

        // ✅ 6. MAP USER INFO (QUAN TRỌNG NHẤT)
        // Logic: Ưu tiên lấy thông tin "Customer" lưu trong Booking (người ở thực tế).
        // Nếu Booking cũ chưa có, mới lấy từ User Account.

        String finalName = b.getCustomerName();
        String finalEmail = b.getCustomerEmail();
        String finalPhone = b.getCustomerPhone();

        // Fallback: Nếu trong Booking null thì lấy từ User gốc
        if (b.getUser() != null) {
            if (finalName == null) finalName = b.getUser().getFullName();
            if (finalEmail == null) finalEmail = b.getUser().getEmail();
            if (finalPhone == null) finalPhone = b.getUser().getPhoneNumber();
        }

        BookingResponseDTO.UserSummaryDto userDto = new BookingResponseDTO.UserSummaryDto(
                b.getUser() != null ? b.getUser().getUserId() : null,
                finalName,  // Tên người ở
                finalEmail, // Email người ở
                finalPhone  // SĐT người ở
        );
        dto.setUser(userDto);

        return dto;
    }

    // ================================
    // 🔍 LẤY LỊCH BẬN CỦA PHÒNG
    // ================================
    public List<Map<String, String>> getRoomAvailability(int roomId) {
        // Lấy tất cả booking từ ngày hôm nay trở đi
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepo.findFutureBookingsByRoomId(roomId, today);

        // Chuyển đổi sang List Map đơn giản: [{start: "2023-12-01", end: "2023-12-05"}, ...]
        return bookings.stream().map(b -> {
            Map<String, String> range = new HashMap<>();
            range.put("start", b.getCheckInDate().toString());
            range.put("end", b.getCheckOutDate().toString());
            return range;
        }).collect(Collectors.toList());
    }

    // =====================================================
    // ÁP DỤNG MÃ GIẢM GIÁ (LOGIC CỘNG DỒN: OWNER TRƯỚC -> ADMIN SAU)
    // =====================================================
    @Transactional
    public BookingResponseDTO applyPromotion(int bookingId, String code) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ có thể áp dụng mã cho đơn hàng chưa thanh toán.");
        }

        // 1. Tìm thông tin mã (Validate cơ bản: tồn tại, còn hạn, active)
        Promotion promotion = promotionRepo.findValidPromotion(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ hoặc đã hết hạn."));

        // 2. Phân loại mã (Admin hay Owner) và lưu vào Booking
        if (promotion.getProperty() == null) {
            // >>> Mã ADMIN
            booking.setAdminPromotionCode(code);
        } else {
            // >>> Mã OWNER
            // Validate: Mã này có phải của khách sạn này không?
            if (promotion.getProperty().getPropertyId() != booking.getProperty().getPropertyId()) {
                throw new RuntimeException("Mã giảm giá này không áp dụng cho khách sạn hiện tại.");
            }
            booking.setPromotionCode(code);
        }

        // 3. Tính toán lại toàn bộ giá (Recalculate)
        calculateAndSetBookingPrice(booking);

        Booking saved = bookingRepo.save(booking);

        // 4. Đồng bộ giá sang bảng Payment
        updatePaymentAmount(saved);

        return convertToDTO(saved);
    }

    // --- Helper: Tính toán giá theo thứ tự ưu tiên ---
    // --- Helper: Tính toán giá theo thứ tự ưu tiên ---
    private void calculateAndSetBookingPrice(Booking booking) {
        // A. Tính giá gốc (Base Price) từ phòng và số đêm
        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (nights <= 0) nights = 1;
        BigDecimal basePrice = booking.getRoom().getPricePerNight().multiply(BigDecimal.valueOf(nights));

        BigDecimal currentPrice = basePrice;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // B. BƯỚC 1: Áp dụng mã OWNER (nếu có)
        if (booking.getPromotionCode() != null) {
            // [SỬA]: Dùng findValidPromotion để check kỹ ngày Start/End/Active ngay tại đây
            Promotion ownerPromo = promotionRepo.findValidPromotion(booking.getPromotionCode(), LocalDateTime.now()).orElse(null);

            if (ownerPromo != null) {
                // Check thêm điều kiện mã phải thuộc về khách sạn này (Double check)
                if (ownerPromo.getProperty() != null && ownerPromo.getProperty().getPropertyId() == booking.getProperty().getPropertyId()) {
                    if (checkMinAmount(ownerPromo, basePrice)) {
                        BigDecimal discount1 = calculateDiscount(basePrice, ownerPromo); // Giảm trên giá gốc

                        // Cập nhật giá
                        currentPrice = currentPrice.subtract(discount1);
                        totalDiscount = totalDiscount.add(discount1);
                    }
                }
            }
        }

        // C. BƯỚC 2: Áp dụng mã ADMIN (nếu có) -> Tính trên GIÁ ĐÃ GIẢM (currentPrice)
        if (booking.getAdminPromotionCode() != null) {
            // [SỬA]: Dùng findValidPromotion cho mã Admin luôn
            Promotion adminPromo = promotionRepo.findValidPromotion(booking.getAdminPromotionCode(), LocalDateTime.now()).orElse(null);

            if (adminPromo != null) {
                // Check mã Admin (property null)
                if (adminPromo.getProperty() == null) {
                    if (checkMinAmount(adminPromo, currentPrice)) {
                        BigDecimal discount2 = calculateDiscount(currentPrice, adminPromo); // Giảm trên giá còn lại

                        currentPrice = currentPrice.subtract(discount2);
                        totalDiscount = totalDiscount.add(discount2);
                    }
                }
            }
        }

        // D. Set giá trị cuối cùng vào Booking (Không âm)
        if (currentPrice.compareTo(BigDecimal.ZERO) < 0) currentPrice = BigDecimal.ZERO;

        booking.setTotalPrice(currentPrice);
        booking.setDiscountAmount(totalDiscount);
    }
    // --- Helper: Tính tiền giảm ---
    private BigDecimal calculateDiscount(BigDecimal amountToApply, Promotion promo) {
        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = promo.getDiscountValue();
        } else {
            // Giảm theo %
            discount = amountToApply.multiply(promo.getDiscountValue()).divide(BigDecimal.valueOf(100));
            // Check Max Discount
            if (promo.getMaxDiscountAmount() != null && discount.compareTo(promo.getMaxDiscountAmount()) > 0) {
                discount = promo.getMaxDiscountAmount();
            }
        }
        // Không giảm quá số tiền hiện tại
        return discount.compareTo(amountToApply) > 0 ? amountToApply : discount;
    }

    private boolean checkMinAmount(Promotion promo, BigDecimal amount) {
        return promo.getMinBookingAmount() == null || amount.compareTo(promo.getMinBookingAmount()) >= 0;
    }

    private void updatePaymentAmount(Booking booking) {
        Payment payment = paymentRepo.findByBooking_BookingId(booking.getBookingId()).orElse(null);
        if (payment != null) {
            payment.setTotalAmount(booking.getTotalPrice());
            paymentRepo.save(payment);
        }
    }

    // ============================================================
    // 🔥 [NEW] XỬ LÝ THANH TOÁN THÀNH CÔNG (Được gọi từ PaymentController/Service)
    // ============================================================
    @Transactional // Nên thêm Transactional nếu chưa có ở class level
    public void confirmBookingPayment(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // 1. Kiểm tra trạng thái hợp lệ
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            logger.warn("Booking {} đã được xử lý trước đó. Trạng thái hiện tại: {}", bookingId, booking.getStatus());
            return;
        }

        // 2. Cập nhật trạng thái Booking -> CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);

        // 3. Cập nhật trạng thái Payment -> PAID/APPROVED
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
        if (payment != null) {
            payment.setPaymentStatus(PaymentStatus.APPROVED);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepo.save(payment);
        }

        bookingRepo.save(booking);
        logger.info("✅ Booking {} đã được xác nhận thanh toán thành công.", bookingId);

        // 4. Gửi email xác nhận đặt phòng (Giữ nguyên logic cũ)
        try {
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
            emailService.sendBookingConfirmationEmail(emailTo, nameTo, String.valueOf(bookingId));
        } catch (Exception e) {
            logger.error("Lỗi gửi email xác nhận booking: {}", e.getMessage());
        }

        // ========================================================================
        // 🔥 [CẬP NHẬT] GỬI THÔNG BÁO (NOTIFICATION)
        // ========================================================================
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String propertyName = booking.getProperty().getPropertyName();
            String priceFormatted = String.format("%,.0f", booking.getTotalPrice());

            // A. Gửi cho OWNER (Người nhận tiền/đơn)
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner.getUserId(), // [SỬA] Lấy String ID
                        "Thanh toán thành công #" + booking.getBookingId(),
                        "Khách hàng " + booking.getCustomerName() + " đã thanh toán " + priceFormatted + " VNĐ. Đơn hàng đã được xác nhận!",
                        NotificationType.BOOKING_RECEIVED, // [SỬA] Dùng Type của Owner
                        relatedId
                );
            }

            // B. [MỚI] Gửi cho CUSTOMER (Người đặt)
            // Đây là thông báo quan trọng nhất để khách biết mình đã có phòng
            User customer = booking.getUser();
            if (customer != null) {
                notificationService.sendNotification(
                        customer.getUserId(), // [SỬA] Lấy String ID
                        "Đặt phòng thành công!",
                        "Chúc mừng! Đơn phòng #" + booking.getBookingId() + " tại " + propertyName + " đã được xác nhận. Chúc bạn có kỳ nghỉ vui vẻ!",
                        NotificationType.BOOKING_SUCCESS, // [SỬA] Dùng Type của Customer (Màu xanh)
                        relatedId
                );
            }

        } catch (Exception e) {
            logger.error("Lỗi gửi thông báo thanh toán: {}", e.getMessage());
        }

        // 5. Kiểm tra gửi mail nhắc nhở check-in ngay lập tức (Giữ nguyên)
        checkAndSendImmediateReminder(booking);
    }

    // ============================================================
    // 🔥 SCHEDULER: QUÉT VÀ GỬI EMAIL NHẮC CHECK-IN (Chạy 8h sáng hàng ngày)
    // ============================================================
    @Transactional
    public void sendCheckinReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // Tìm các đơn CONFIRMED check-in ngày mai
        List<Booking> bookings = bookingRepo.findByCheckInDateAndStatus(tomorrow, BookingStatus.CONFIRMED);

        if (!bookings.isEmpty()) {
            logger.info("🔍 Tìm thấy {} đơn hàng check-in ngày mai ({})", bookings.size(), tomorrow);

            for (Booking booking : bookings) {
                try {
                    String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                    String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
                    String propertyName = booking.getProperty().getPropertyName();

                    emailService.sendCheckinReminderEmail(
                            emailTo,
                            nameTo,
                            String.valueOf(booking.getBookingId()),
                            propertyName,
                            booking.getCheckInDate().toString()
                    );
                    logger.info("✅ Đã gửi email nhắc check-in (Scheduler) cho Booking ID: {}", booking.getBookingId());

                } catch (Exception e) {
                    logger.error("❌ Lỗi gửi email nhắc check-in (Scheduler) cho ID {}: {}", booking.getBookingId(), e.getMessage());
                }
            }
        } else {
            logger.info("📅 Không có đơn hàng nào check-in vào ngày mai ({})", tomorrow);
        }
    }

    // ============================================================
    // 🔥 HELPER: KIỂM TRA VÀ GỬI MAIL NHẮC CHECK-IN NGAY (Real-time)
    // ============================================================
    public void checkAndSendImmediateReminder(Booking booking) {
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Logic: Nếu khách đặt phòng Check-in là HÔM NAY hoặc NGÀY MAI
        // thì gửi email nhắc nhở ngay lập tức (vì đã lỡ hoặc sắp tới giờ Scheduler chạy)
        if (checkInDate.isEqual(today) || checkInDate.isEqual(tomorrow)) {
            try {
                String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();
                String propertyName = booking.getProperty().getPropertyName();

                emailService.sendCheckinReminderEmail(
                        emailTo,
                        nameTo,
                        String.valueOf(booking.getBookingId()),
                        propertyName,
                        booking.getCheckInDate().toString()
                );
                logger.info("⚡ Đã gửi email nhắc nhở NGAY LẬP TỨC cho Booking ID: {}", booking.getBookingId());
            } catch (Exception e) {
                logger.error("❌ Lỗi gửi email nhắc nhở ngay lập tức: {}", e.getMessage());
            }
        }
    }
}

