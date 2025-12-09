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
import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.repository.NotificationRepository;

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
    private final NotificationRepository notificationRepo;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // ================================
    // CREATE BOOKING (chặt chẽ, capacity cho mọi loại)
    // ================================
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO req) {

        // --- 1) Kiểm tra tồn tại cơ bản ---
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
        booking.setUser(user);          // Tài khoản đặt
        booking.setProperty(property);
        booking.setRoom(room);
        booking.setCheckInDate(req.getCheckInDate());
        booking.setCheckOutDate(req.getCheckOutDate());
        booking.setGuestCount(req.getGuestCount());
        booking.setTotalPrice(total);
        booking.setPenaltyAmount(BigDecimal.ZERO);
        booking.setRefundAmount(BigDecimal.ZERO); // Init bằng 0 thay vì total
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        // 🔥 [LOGIC MỚI] Xử lý thông tin người ở
        if (req.isBookingForSelf()) {
            // Nếu đặt cho mình: Lấy thông tin từ User Detail trong DB (đảm bảo chính xác)
            booking.setCustomerName(user.getFullName());
            booking.setCustomerPhone(user.getPhoneNumber());
            booking.setCustomerEmail(user.getEmail());
        } else {
            // Nếu đặt hộ: Lấy thông tin người dùng nhập từ Form
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

        // Gửi email xác nhận đã nhận yêu cầu (Pending Payment)
        try {
            emailService.sendPaymentReminderEmail(
                    booking.getCustomerEmail(), // Gửi vào email người ở (hoặc user.getEmail() tuỳ logic)
                    booking.getCustomerName(),
                    String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice().toString()
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }
        createNotification(
                booking.getUser(),
                "Đơn đặt phòng đang chờ thanh toán",
                String.format("Đơn hàng #%d đã được tạo thành công và đang chờ bạn thanh toán trong vòng 5 phút.", booking.getBookingId()),
                "BOOKING_PENDING"
        );
        return convertToDTO(booking);
    }

    // ================================
    // CANCEL BOOKING
    // ================================
    @Transactional
    public BookingResponseDTO cancelBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        // Nếu chưa thanh toán -> Hủy ngay lập tức, không tính phạt, không hoàn tiền
        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPenaltyAmount(BigDecimal.ZERO);
            booking.setRefundAmount(BigDecimal.ZERO);

            // Cập nhật trạng thái Payment sang FAILED (nếu đã tạo record payment)
            Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.REJECTED);
                paymentRepo.save(payment);
            }

            Booking saved = bookingRepo.save(booking);
            createNotification(
                    booking.getUser(),
                    "Hủy đơn đặt phòng thành công",
                    String.format("Đơn hàng #%d đã được hủy thành công do chưa thanh toán.", booking.getBookingId()),
                    "BOOKING_CANCELLED"
            );
            return convertToDTO(saved);
        }

        // --- 1. Logic tính toán hoàn tiền (Lấy từ code gốc của bạn) ---
        PropertyPolicies policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = booking.getCheckInDate();

        BigDecimal refundAmount;
        BigDecimal penaltyAmount;

        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, checkInDate);

        if (daysUntilCheckIn <= 1) {
            // Trường hợp 1: Sát ngày (<= 1 ngày) -> Phạt 100%
            penaltyAmount = booking.getTotalPrice();
            refundAmount = BigDecimal.ZERO;
        } else {
            boolean isFreeCancellation = false;
            // Check chính sách miễn phí của Property
            if (policies != null && Boolean.TRUE.equals(policies.isAllowFreeCancellation())) {
                Integer freeDays = policies.getFreeCancellationDays();
                if (freeDays == null) freeDays = 0;
                LocalDate freeDeadline = checkInDate.minusDays(freeDays);

                if (!today.isAfter(freeDeadline)) {
                    isFreeCancellation = true;
                }
            }

            if (isFreeCancellation) {
                // Trường hợp 2: Miễn phí -> Hoàn 100%
                penaltyAmount = BigDecimal.ZERO;
                refundAmount = booking.getTotalPrice();
            } else {
                // Trường hợp 3: Ngoài chính sách -> Phạt 30%
                penaltyAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.30));
                refundAmount = booking.getTotalPrice().subtract(penaltyAmount);
            }
        }

        // 3. Cập nhật Booking
        booking.setPenaltyAmount(penaltyAmount);
        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);

        // 4. TỰ ĐỘNG TẠO YÊU CẦU HOÀN TIỀN (Nếu có tiền hoàn và đã thanh toán)
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0 && payment.getPaymentStatus() == PaymentStatus.APPROVED) {

            if (!refundRepo.existsByBooking(booking)) {
                RefundRequest refund = new RefundRequest();
                refund.setBooking(booking);
                refund.setAmount(refundAmount);
                refund.setStatus(RefundRequestStatus.PENDING); // Chờ Admin duyệt
                refund.setReason("Khách hủy phòng (Hệ thống tự động tạo)");
                refund.setRequestDate(LocalDateTime.now());

                // Lưu RefundRequest (không cần bank info vì user ko nhập)
                refundRepo.save(refund);

                // Cập nhật Payment -> REFUND_REQUESTED
                payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);
                paymentRepo.save(payment);
                createNotification(
                        booking.getUser(),
                        "Yêu cầu hủy phòng đã được gửi",
                        String.format("Hệ thống đã nhận yêu cầu hủy đơn #%d và đang chờ quản trị viên duyệt hoàn tiền.", booking.getBookingId()),
                        "CANCELLATION_REQUESTED"
                );
            }
        }

        // 5. Gửi Email thông báo "Chờ duyệt"
        try {
            // Ưu tiên gửi cho email khách hàng nhập trong booking
            String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
            String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

            emailService.sendCancellationRequestReceivedEmail(
                    emailTo,
                    nameTo,
                    String.valueOf(booking.getBookingId()),
                    booking.getTotalPrice(),
                    penaltyAmount,
                    refundAmount
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail cancel: " + e.getMessage());
        }
        createNotification(
                booking.getUser(),
                "Hủy đơn đặt phòng thành công",
                buildCancelMessage(booking, refundAmount, penaltyAmount),
                "BOOKING_CANCELLED"
        );
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
                    createNotification(
                            booking.getUser(),
                            "Đơn hàng bị hủy tự động",
                            String.format("Đơn hàng #%d đã bị hủy do quá thời gian thanh toán cho phép.", booking.getBookingId()),
                            "BOOKING_EXPIRED"
                    );
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
            createNotification(
                    booking.getUser(),
                    "Hoàn tiền đã được duyệt",
                    String.format("Yêu cầu hoàn tiền cho đơn #%d đã được duyệt.", booking.getBookingId()),
                    "REFUND_APPROVED"

                    );
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
        createNotification(
                booking.getUser(),
                "Check-in thành công",
                String.format("Bạn đã Check-in thành công tại %s.", booking.getProperty().getPropertyName()),
                "CHECK_IN"
        );
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
        MembershipRank oldRank = booking.getUser().getMembershipRank();
        int earnedPoints = 0; // Khai báo earnedPoints ở ngoài để nó có thể được sử dụng ở cuối hàm

        // 2. TÍCH ĐIỂM & THĂNG HẠNG
        if (booking.getTotalPrice() != null) {
            User user = booking.getUser();

            // Tính điểm: 1000 VND = 1 điểm (Lấy phần nguyên)
            earnedPoints = booking.getTotalPrice().divide(BigDecimal.valueOf(1000)).intValue();

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
        String message = String.format("Đơn hàng #%d đã hoàn tất. Bạn đã tích lũy thêm %d điểm.", booking.getBookingId(), earnedPoints);
        if (oldRank != booking.getUser().getMembershipRank()) {
            message += String.format(" Bạn đã được thăng hạng thành viên lên %s!", booking.getUser().getMembershipRank());
        }

        createNotification(
                booking.getUser(),
                "Hoàn tất đơn hàng & Tích điểm",
                message,
                "CHECKOUT_COMPLETED"
        );
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
    // ÁP DỤNG MÃ GIẢM GIÁ
    // =====================================================
    @Transactional
    public BookingResponseDTO applyPromotion(int bookingId, String code) {
        // 1. Tìm Booking
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ có thể áp dụng mã cho đơn hàng chưa thanh toán.");
        }

        // 2. Tính lại GIÁ GỐC (Original Price) để tránh lỗi áp dụng chồng mã
        // Giá gốc = Giá hiện tại + Giá đã giảm trước đó (nếu có)
        BigDecimal currentTotal = booking.getTotalPrice();
        BigDecimal currentDiscount = booking.getDiscountAmount() == null ? BigDecimal.ZERO : booking.getDiscountAmount();
        BigDecimal originalPrice = currentTotal.add(currentDiscount);

        // 3. Tìm và Validate Promotion
        // Hàm findValidPromotion đã có sẵn trong PromotionRepository (kiểm tra ngày, status, limit)
        Promotion promotion = promotionRepo.findValidPromotion(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ, đã hết hạn hoặc hết lượt sử dụng."));

        // 4. Validate điều kiện: Giá trị đơn tối thiểu
        if (promotion.getMinBookingAmount() != null
                && originalPrice.compareTo(promotion.getMinBookingAmount()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để dùng mã này ("
                    + String.format("%,.0f", promotion.getMinBookingAmount()) + " VND)");
        }

        // 5. Tính toán Discount
        BigDecimal discount = BigDecimal.ZERO;

        if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            // Giảm tiền mặt
            discount = promotion.getDiscountValue();
        } else {
            // Giảm theo %
            discount = originalPrice.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));

            // Kiểm tra số tiền giảm tối đa (Max Discount)
            if (promotion.getMaxDiscountAmount() != null
                    && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
        }

        // Đảm bảo không giảm quá giá trị đơn (không âm tiền)
        if (discount.compareTo(originalPrice) > 0) {
            discount = originalPrice;
        }

        // 6. Cập nhật Booking
        BigDecimal newTotal = originalPrice.subtract(discount);

        booking.setPromotionCode(code);
        booking.setDiscountAmount(discount);
        booking.setTotalPrice(newTotal);

        // Cập nhật cả bảng Payment (vì Payment lưu totalAmount)
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
        if (payment != null) {
            payment.setTotalAmount(newTotal);
            paymentRepo.save(payment);
        }

        Booking saved = bookingRepo.save(booking);
        return convertToDTO(saved);
    }

    private void createNotification(User user, String title, String message, String type) {
        if (user == null) return;

        boolean shouldCheckDuplicate =
                "BOOKING_EXPIRED".equals(type);   // muốn chặn thêm type nào thì OR thêm ở đây

        if (shouldCheckDuplicate) {
            boolean exists = notificationRepo
                    .existsByUserUserIdAndTypeAndMessage(user.getUserId(), type, message);

            if (exists) {
                logger.info("Skip duplicate AUTO notification for user {} - type: {}, msg: {}",
                        user.getUserId(), type, message);
                return;
            }
        }

        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .user(user)
                .build();

        notificationRepo.save(notification);

    }
    private String buildCancelMessage(Booking booking,
                                      BigDecimal refundAmount,
                                      BigDecimal penaltyAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Đơn hàng #%d đã được hủy thành công.", booking.getBookingId()));

        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format(" Bạn sẽ được hoàn khoảng %,.0f₫.", refundAmount));
        }
        if (penaltyAmount != null && penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format(" Phí phạt khoảng %,.0f₫.", penaltyAmount));
        }
        return sb.toString();
    }

}
