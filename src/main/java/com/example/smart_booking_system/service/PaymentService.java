package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.entity.Notification;
import com.example.smart_booking_system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RefundRequestRepository refundRepo;
    private final EmailService emailService;
    private final PromotionRepository promotionRepo;
    private final NotificationRepository notificationRepo;

    // =========================================================================
    // 1. THANH TOÁN THÀNH CÔNG (submitPayment)
    // =========================================================================
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, String note, String paymentMethod) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new RuntimeException("Payment info not found"));

        // ... Logic Promotion và Validate (Giữ nguyên) ...
        if (booking.getPromotionCode() != null) {
            promotionRepo.findValidPromotion(booking.getPromotionCode(), LocalDateTime.now()).ifPresent(promo -> {
                promo.setUsageCount(promo.getUsageCount() + 1);
                promotionRepo.save(promo);
            });
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) return ApiResponse.error("Đơn hàng không ở trạng thái chờ thanh toán.");

        // ... Cập nhật Payment và Booking (Giữ nguyên) ...
        payment.setPaymentMethod(paymentMethod);
        payment.setTotalAmount(booking.getTotalPrice());
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setConfirmedDate(LocalDateTime.now());
        String trxRef = "TRX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setTransactionReference(trxRef);
        payment.setNote(note);
        paymentRepo.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking);

        // ✅ TẠO THÔNG BÁO THANH TOÁN THÀNH CÔNG
        createNotification(
                booking.getUser(),
                "Đặt phòng thành công",
                String.format("Đơn hàng #%d đã được xác nhận và thanh toán thành công.", booking.getBookingId()),
                "BOOKING_CONFIRMED"
        );

        sendConfirmationEmail(booking, trxRef);
        return ApiResponse.success("Thanh toán thành công!", new PaymentResponseDTO(payment, null));
    }

    // =========================================================================
    // 2. NGƯỜI DÙNG GỬI YÊU CẦU HOÀN TIỀN (requestRefundByUser)
    // =========================================================================
    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, RefundSubmitDTO req) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new RuntimeException("Payment not found"));

        if (booking.getStatus() != BookingStatus.CANCELLED) return ApiResponse.error("Phải Hủy phòng trước khi yêu cầu.");
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) return ApiResponse.error("Chưa thanh toán thành công.");
        if (refundRepo.existsByBooking(booking)) return ApiResponse.error("Đã có yêu cầu đang chờ xử lý.");

        // ... Tạo Refund Request và Cập nhật Payment (Giữ nguyên) ...
        RefundRequest refund = new RefundRequest();
        refund.setBooking(booking);
        refund.setAmount(booking.getRefundAmount());
        refund.setReason(req.getReason());
        refund.setBankName(req.getBankName());
        refund.setAccountNumber(req.getAccountNumber());
        refund.setAccountHolder(req.getAccountHolder());
        refund.setStatus(RefundRequestStatus.PENDING);
        refund.setRequestDate(LocalDateTime.now());
        refundRepo.save(refund);

        payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);
        paymentRepo.save(payment);

        // ✅ TẠO THÔNG BÁO YÊU CẦU HOÀN TIỀN ĐÃ GHI NHẬN
        createNotification(
                booking.getUser(),
                "Yêu cầu hoàn tiền đã được gửi",
                "Yêu cầu hoàn tiền của bạn đang chờ quản trị viên xem xét và xử lý.",
                "REFUND_REQUESTED_USER"
        );
        return ApiResponse.success("Gửi yêu cầu thành công", new PaymentResponseDTO(payment, refund));
    }

    // =========================================================================
    // 3. ADMIN DUYỆT/TỪ CHỐI HOÀN TIỀN (processRefund)
    // =========================================================================
    @Transactional
    public ApiResponse<?> processRefund(int refundRequestId, boolean isApproved, String adminNote) {
        RefundRequest refund = refundRepo.findById(refundRequestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));

        Booking booking = refund.getBooking();
        Payment payment = paymentRepo.findByBooking_BookingId(booking.getBookingId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (refund.getStatus() != RefundRequestStatus.PENDING) {
            return ApiResponse.error("Yêu cầu này đã được xử lý trước đó.");
        }

        String title, message, type;

        if (isApproved) {
            refund.setStatus(RefundRequestStatus.APPROVED);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(refund.getAmount());

            title = "Hoàn tiền đã được duyệt";
            message = String.format("Yêu cầu hoàn tiền cho đơn #%d đã được duyệt. Số tiền %,.0f sẽ được chuyển khoản.", booking.getBookingId(), refund.getAmount());
            type = "REFUND_APPROVED";
        } else {
            refund.setStatus(RefundRequestStatus.REJECTED);
            payment.setPaymentStatus(PaymentStatus.APPROVED);

            title = "Yêu cầu hoàn tiền bị từ chối";
            message = "Yêu cầu hoàn tiền của bạn bị từ chối. Lý do: " + adminNote;
            type = "REFUND_REJECTED";
        }

        refund.setResolveDate(LocalDateTime.now());
        refund.setAdminNote(adminNote);
        refundRepo.save(refund);
        paymentRepo.save(payment);

        // ✅ Gọi hàm Notification sau khi định nghĩa biến
        createNotification(booking.getUser(), title, message, type);

        // ✅ LOGIC GỬI EMAIL
        try {
            if (isApproved) {
                // Lấy thông tin người nhận (ưu tiên customer info trong booking)
                String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

                emailService.sendCancellationSuccessEmail(
                        emailTo,
                        nameTo,
                        String.valueOf(booking.getBookingId()),
                        String.format("%,.0f", refund.getAmount()), // Số tiền hoàn
                        String.format("%,.0f", booking.getPenaltyAmount()) // Phí phạt
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi email hoàn tiền: " + e.getMessage());
        }

        return ApiResponse.success(isApproved ? "Đã duyệt hoàn tiền" : "Đã từ chối hoàn tiền", null);
    }

    // =========================================================================
    // CÁC HÀM GET (Giữ nguyên)
    // =========================================================================
    public List<PaymentResponseDTO> getUserTransactionHistory(String userId) {
        List<Payment> payments = paymentRepo.findByBooking_User_UserIdOrderByPaymentDateDesc(userId);
        return payments.stream().map(p -> new PaymentResponseDTO(p, refundRepo.findByBooking(p.getBooking()).orElse(null))).collect(Collectors.toList());
    }

    public List<PaymentResponseDTO> getAllTransactions() {
        return paymentRepo.findAllByOrderByPaymentDateDesc().stream().map(p -> new PaymentResponseDTO(p, refundRepo.findByBooking(p.getBooking()).orElse(null))).collect(Collectors.toList());
    }

    private void sendConfirmationEmail(Booking booking, String trxRef) {
        try {
            emailService.sendBookingConfirmationEmail(booking.getUser().getEmail(), booking.getUser().getFullName(), String.valueOf(booking.getBookingId()));
        } catch (Exception e) { System.err.println("Error mail: " + e.getMessage()); }
    }

    private void createNotification(User user, String title, String message, String type) {
        try {
            Notification notification = Notification.builder()
                    .title(title)
                    .message(message)
                    .type(type)
                    .isRead(false)
                    .user(user)
                    .build();
            notificationRepo.save(notification);
        } catch (Exception e) {
            System.err.println("Lỗi tạo thông báo (Notification): " + e.getMessage());
        }
    }
}