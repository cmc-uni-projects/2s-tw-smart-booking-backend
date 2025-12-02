package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final PropertyRepository propertyRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final RatingRepository ratingRepo;
    private final PropertyPoliciesRepository policiesRepo;
    private final EmailService emailService;
    private final PaymentRepository paymentRepo;
    private final PromotionRepository promotionRepo;
    private final RefundRequestRepository refundRepo;
    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // =========================================================
    // 🔥 LOGIC THỐNG KÊ DASHBOARD (ADMIN & OWNER)
    // =========================================================

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> response = new HashMap<>();

        // 1. Thống kê tổng quan
        BigDecimal totalRevenue = bookingRepo.calculateGlobalRevenue();
        response.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        long totalBookings = bookingRepo.countTotalConfirmedBookings();
        response.put("totalBookings", totalBookings);

        long totalProperties = propertyRepo.count();
        response.put("totalProperties", totalProperties);

        long totalRooms = roomRepo.count();
        response.put("totalRooms", totalRooms);

        // Lưu ý: Đảm bảo RatingRepository đã có hàm countByIsHidden(boolean)
        long totalReviews = ratingRepo.countByIsHidden(false);
        response.put("totalReviews", totalReviews);

        long newBookings = bookingRepo.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));
        response.put("newBookings", newBookings);

        long totalUsers = userRepo.count();
        response.put("totalUsers", totalUsers);

        // 2. Biểu đồ doanh thu theo tháng
        List<Booking> bookingsThisYear = bookingRepo.findGlobalBookingsByYear(LocalDate.now().getYear());
        Map<Integer, BigDecimal> monthlyRevenue = new HashMap<>();
        for (int i = 1; i <= 12; i++) monthlyRevenue.put(i, BigDecimal.ZERO);

        for (Booking b : bookingsThisYear) {
            int month = b.getCreatedAt().getMonthValue();
            monthlyRevenue.put(month, monthlyRevenue.get(month).add(b.getTotalPrice()));
        }

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<Map<String, Object>> revenueData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", monthNames[i-1]);
            dataPoint.put("revenue", monthlyRevenue.get(i));
            revenueData.add(dataPoint);
        }
        response.put("revenueData", revenueData);

        // 3. Top Khách sạn doanh thu cao nhất
        List<Object[]> topProps = bookingRepo.findTopPropertiesGlobal(PageRequest.of(0, 5));
        List<Map<String, Object>> topHotels = topProps.stream().map(obj -> {
            Property p = (Property) obj[0];
            Long count = (Long) obj[1];
            BigDecimal rev = (BigDecimal) obj[2];
            Map<String, Object> hotelMap = new HashMap<>();
            hotelMap.put("id", p.getPropertyId());
            hotelMap.put("name", p.getPropertyName());
            hotelMap.put("bookings", count);
            hotelMap.put("revenue", rev);
            // Lấy ảnh đầu tiên nếu có
            String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getImageUrl() : "";
            hotelMap.put("image", img);
            return hotelMap;
        }).collect(Collectors.toList());
        response.put("topHotels", topHotels);

        // 4. Danh sách Booking gần đây (Mới nhất)
        Page<Booking> recentPage = bookingRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5));
        List<BookingResponseDTO> recentBookings = recentPage.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        response.put("recentBookings", recentBookings);

        return response;
    }

    public Map<String, Object> getOwnerDashboardStats(String ownerId) {
        Map<String, Object> response = new HashMap<>();

        // 1. Thống kê cơ bản
        BigDecimal totalRevenue = bookingRepo.calculateOwnerRevenue(ownerId);
        response.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        long totalBookings = bookingRepo.countByProperty_Owner_UserId(ownerId);
        response.put("totalBookings", totalBookings);

        long newBookings = bookingRepo.countByProperty_Owner_UserIdAndCreatedAtAfter(ownerId, LocalDateTime.now().minusDays(30));
        response.put("newBookings", newBookings);

        // Placeholder cho công suất phòng (có thể tính sau)
        response.put("occupancyRate", 0);

        // 2. Biểu đồ doanh thu
        List<Booking> bookingsThisYear = bookingRepo.findOwnerBookingsByYear(ownerId, LocalDate.now().getYear());
        Map<Integer, BigDecimal> monthlyRevenue = new HashMap<>();
        for (int i = 1; i <= 12; i++) monthlyRevenue.put(i, BigDecimal.ZERO);
        for (Booking b : bookingsThisYear) {
            int month = b.getCreatedAt().getMonthValue();
            monthlyRevenue.put(month, monthlyRevenue.get(month).add(b.getTotalPrice()));
        }

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<Map<String, Object>> revenueData = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", monthNames[i-1]);
            dataPoint.put("revenue", monthlyRevenue.get(i));
            revenueData.add(dataPoint);
        }
        response.put("revenueData", revenueData);

        // 3. Booking gần đây của Owner
        List<Booking> recent = bookingRepo.findByProperty_Owner_UserIdOrderByCreatedAtDesc(ownerId, PageRequest.of(0, 5));
        response.put("recentBookings", recent.stream().map(this::convertToDTO).collect(Collectors.toList()));

        return response;
    }

    // =========================================================
    // CÁC HÀM XỬ LÝ BOOKING (CREATE, CANCEL...) - GIỮ NGUYÊN
    // =========================================================

    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO req) {
        User user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));

        Property property = propertyRepo.findById(req.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found: " + req.getPropertyId()));

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

        if (req.getGuestCount() == null || req.getGuestCount() <= 0) {
            throw new RuntimeException("guestCount must be greater than 0");
        }
        if (req.getGuestCount() > room.getCapacity()) {
            throw new RuntimeException("guestCount exceeds room capacity");
        }

        if (req.getCheckInDate() == null || req.getCheckOutDate() == null) {
            throw new RuntimeException("Dates are required");
        }
        if (!req.getCheckInDate().isBefore(req.getCheckOutDate())) {
            throw new RuntimeException("checkInDate must be before checkOutDate");
        }

        List<Booking> overlapping = bookingRepo.findConfirmedOverlappingByRoomId(
                room.getRoomId(), req.getCheckInDate(), req.getCheckOutDate()
        );
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Room is already booked in the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(req.getCheckInDate(), req.getCheckOutDate());
        if (nights <= 0) nights = 1;
        BigDecimal total = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

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

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTotalAmount(total);
        payment.setPaymentMethod(null);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

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

        return convertToDTO(booking);
    }

    @Transactional
    public BookingResponseDTO cancelBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already cancelled");
        }

        if (booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setPenaltyAmount(BigDecimal.ZERO);
            booking.setRefundAmount(BigDecimal.ZERO);

            Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
            if (payment != null) {
                payment.setPaymentStatus(PaymentStatus.REJECTED);
                paymentRepo.save(payment);
            }

            Booking saved = bookingRepo.save(booking);
            return convertToDTO(saved);
        }

        PropertyPolicies policies = policiesRepo.findByPropertyId(booking.getProperty().getPropertyId());
        LocalDate today = LocalDate.now();
        LocalDate checkInDate = booking.getCheckInDate();

        BigDecimal refundAmount;
        BigDecimal penaltyAmount;

        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, checkInDate);

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
                penaltyAmount = booking.getTotalPrice().multiply(BigDecimal.valueOf(0.30));
                refundAmount = booking.getTotalPrice().subtract(penaltyAmount);
            }
        }

        booking.setPenaltyAmount(penaltyAmount);
        booking.setRefundAmount(refundAmount);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0 && payment.getPaymentStatus() == PaymentStatus.APPROVED) {
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
            }
        }

        try {
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

        return convertToDTO(booking);
    }

    @Transactional
    public void scanAndCancelExpiredBookings() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(5);

        List<Booking> expiredBookings = bookingRepo.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT,
                expirationTime
        );

        if (!expiredBookings.isEmpty()) {
            logger.info("Tìm thấy {} đơn hàng quá hạn thanh toán (5 phút). Đang hủy...", expiredBookings.size());

            for (Booking booking : expiredBookings) {
                try {
                    cancelBooking(booking.getBookingId());
                    logger.info("Đã tự động hủy đơn booking ID: {}", booking.getBookingId());
                } catch (Exception e) {
                    logger.error("Lỗi khi tự động hủy đơn ID {}: {}", booking.getBookingId(), e.getMessage());
                }
            }
        }
    }

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

    @Transactional
    public void checkInBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow();
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ được Check-in đơn ĐÃ XÁC NHẬN");
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        bookingRepo.save(booking);
    }

    @Transactional
    public void checkOutBooking(int bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CHECKED_IN && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Trạng thái không hợp lệ để Check-out");
        }

        booking.setStatus(BookingStatus.COMPLETED);

        if (booking.getTotalPrice() != null) {
            User user = booking.getUser();

            int earnedPoints = booking.getTotalPrice().divide(BigDecimal.valueOf(1000)).intValue();

            if (earnedPoints > 0) {
                int currentPoints = user.getPoints();
                int newTotalPoints = currentPoints + earnedPoints;
                user.setPoints(newTotalPoints);
                updateUserRank(user, newTotalPoints);
                userRepo.save(user);
            }
        }

        bookingRepo.save(booking);
    }

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

    public void updateUserRank(User user, int points) {
        MembershipRank newRank = calculateRankFromPoints(points);
        user.setMembershipRank(newRank);
    }

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

    public List<Map<String, String>> getRoomAvailability(int roomId) {
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepo.findFutureBookingsByRoomId(roomId, today);

        return bookings.stream().map(b -> {
            Map<String, String> range = new HashMap<>();
            range.put("start", b.getCheckInDate().toString());
            range.put("end", b.getCheckOutDate().toString());
            return range;
        }).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponseDTO applyPromotion(int bookingId, String code) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ có thể áp dụng mã cho đơn hàng chưa thanh toán.");
        }

        BigDecimal currentTotal = booking.getTotalPrice();
        BigDecimal currentDiscount = booking.getDiscountAmount() == null ? BigDecimal.ZERO : booking.getDiscountAmount();
        BigDecimal originalPrice = currentTotal.add(currentDiscount);

        Promotion promotion = promotionRepo.findValidPromotion(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ, đã hết hạn hoặc hết lượt sử dụng."));

        if (promotion.getMinBookingAmount() != null
                && originalPrice.compareTo(promotion.getMinBookingAmount()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để dùng mã này ("
                    + String.format("%,.0f", promotion.getMinBookingAmount()) + " VND)");
        }

        BigDecimal discount;

        if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = promotion.getDiscountValue();
        } else {
            discount = originalPrice.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));

            if (promotion.getMaxDiscountAmount() != null
                    && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                discount = promotion.getMaxDiscountAmount();
            }
        }

        if (discount.compareTo(originalPrice) > 0) {
            discount = originalPrice;
        }

        BigDecimal newTotal = originalPrice.subtract(discount);

        booking.setPromotionCode(code);
        booking.setDiscountAmount(discount);
        booking.setTotalPrice(newTotal);

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(null);
        if (payment != null) {
            payment.setTotalAmount(newTotal);
            paymentRepo.save(payment);
        }

        Booking saved = bookingRepo.save(booking);
        return convertToDTO(saved);
    }

    // ==========================================
    // HELPER: CONVERT TO DTO
    // ==========================================
    private BookingResponseDTO convertToDTO(Booking b) {
        BookingResponseDTO dto = new BookingResponseDTO();

        Payment payment = paymentRepo.findByBooking_BookingId(b.getBookingId()).orElse(null);
        if (payment != null) {
            dto.setPaymentStatus(payment.getPaymentStatus().name());
            dto.setPaymentMethod(payment.getPaymentMethod());
        }

        dto.setBookingId(b.getBookingId());
        dto.setPropertyId(b.getProperty().getPropertyId());
        dto.setRoomId(b.getRoom() != null ? b.getRoom().getRoomId() : null);

        dto.setCheckInDate(b.getCheckInDate());
        dto.setCheckOutDate(b.getCheckOutDate());
        dto.setGuestCount(b.getGuestCount());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setPenaltyAmount(b.getPenaltyAmount());
        dto.setRefundAmount(b.getRefundAmount());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setDiscountAmount(b.getDiscountAmount() != null ? b.getDiscountAmount() : BigDecimal.ZERO);
        dto.setPromotionCode(b.getPromotionCode());
        dto.setStatus(b.getStatus());

        dto.setSpecialRequest(b.getSpecialRequest());

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

        if (b.getRoom() != null) {
            dto.setRoomName(b.getRoom().getRoomName());
        }

        String finalName = b.getCustomerName();
        String finalEmail = b.getCustomerEmail();
        String finalPhone = b.getCustomerPhone();

        if (b.getUser() != null) {
            if (finalName == null) finalName = b.getUser().getFullName();
            if (finalEmail == null) finalEmail = b.getUser().getEmail();
            if (finalPhone == null) finalPhone = b.getUser().getPhoneNumber();
        }

        BookingResponseDTO.UserSummaryDto userDto = new BookingResponseDTO.UserSummaryDto(
                b.getUser() != null ? b.getUser().getUserId() : null,
                finalName,
                finalEmail,
                finalPhone
        );
        dto.setUser(userDto);

        return dto;
    }
}