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

        return convertToDTO(booking);
    }

    // ================================
    // CANCEL BOOKING (User thực hiện)
    // ================================
    @Transactional
    public BookingResponseDTO cancelBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        // --- 1. Logic tính toán hoàn tiền (Lấy từ code gốc của bạn) ---
        PropertyPolicies policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        LocalDate today = LocalDate.now();

        boolean allowFree = false;

        if (policies != null && Boolean.TRUE.equals(policies.isAllowFreeCancellation())) {
            Integer freeDays = policies.getFreeCancellationDays();
            if (freeDays == null) freeDays = 0;
            if (freeDays < 0) freeDays = 0;

            LocalDate deadline = booking.getCheckInDate().minusDays(freeDays);
            // Cho phép hủy miễn phí nếu hôm nay <= deadline
            if (!today.isAfter(deadline)) {
                allowFree = true;
            }
        }

        BigDecimal refundAmount;

        if (allowFree) {
            booking.setPenaltyAmount(BigDecimal.ZERO);
            refundAmount = booking.getTotalPrice();
        } else {
            // Phạt 20% nếu hủy muộn
            BigDecimal penalty = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.20));
            booking.setPenaltyAmount(penalty);
            refundAmount = booking.getTotalPrice().subtract(penalty);
        }

        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.CANCELLED); // Đã hủy booking
        bookingRepo.save(booking);


        // --- 3. Gửi Email 1: Thông báo đã nhận yêu cầu hủy ---
        try {
            if (booking.getUser() != null) {
                emailService.sendCancellationRequestReceivedEmail(
                        booking.getUser().getEmail(),
                        booking.getUser().getFullName(),
                        String.valueOf(booking.getBookingId())
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail request cancel: " + e.getMessage());
        }

        return convertToDTO(booking);
    }

    // ================================
    // APPROVE REFUND (Admin thực hiện)
    // ================================
    @Transactional
    public void approveRefund(int bookingId) {
        // 1. Tìm Booking
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // 2. Tìm Payment liên quan
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment info not found for booking: " + bookingId));

        // 3. Kiểm tra logic (chỉ duyệt nếu Booking đã hủy)
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new RuntimeException("Chỉ có thể hoàn tiền cho đơn đã Hủy (CANCELLED)");
        }

        // 4. Cập nhật trạng thái Payment thành REFUNDED
        // (Nhớ import com.example.smart_booking_system.enums.PaymentStatus)
        payment.setPaymentStatus(com.example.smart_booking_system.enums.PaymentStatus.REFUNDED);
        payment.setConfirmedDate(LocalDateTime.now()); // Cập nhật ngày thực hiện hoàn tiền
        payment.setNote(payment.getNote() + " | Admin đã duyệt hoàn tiền ngày " + LocalDateTime.now());

        paymentRepo.save(payment);

        // ✅ [BƯỚC 2 - THÊM MỚI] Gửi Email 2: Xác nhận hoàn tiền thành công
        try {
            if (booking.getUser() != null) {
                emailService.sendCancellationSuccessEmail(
                        booking.getUser().getEmail(),
                        booking.getUser().getFullName(),
                        String.valueOf(booking.getBookingId()),
                        String.format("%,.0f", booking.getRefundAmount()), // Số tiền hoàn
                        String.format("%,.0f", booking.getPenaltyAmount()) // Phí phạt
                );
            }
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

        // 2. 🔥 [LOGIC MỚI] TÍCH ĐIỂM & THĂNG HẠNG
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
    }

    // ================================
    // HELPER: CẬP NHẬT HẠNG THÀNH VIÊN
    // ================================
    private void updateUserRank(User user, int points) {
        if (points >= 10000) {
            user.setMembershipRank(MembershipRank.DIAMOND);
        } else if (points >= 5000) {
            user.setMembershipRank(MembershipRank.GOLD);
        } else if (points >= 1000) {
            user.setMembershipRank(MembershipRank.SILVER);
        } else {
            user.setMembershipRank(MembershipRank.BRONZE);
        }
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
        // ✅ Map thêm thông tin giảm giá
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
}
