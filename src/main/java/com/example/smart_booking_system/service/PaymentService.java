package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

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

    // =================================================================
    // 1. SUBMIT PAYMENT (Khách thanh toán thành công -> Chốt đơn)
    // =================================================================
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, String note, String paymentMethod) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment info not found"));
        if (booking.getPromotionCode() != null) {
            // Tìm khuyến mãi để tăng count
            promotionRepo.findValidPromotion(booking.getPromotionCode(), LocalDateTime.now())
                    .ifPresent(promo -> {
                        promo.setUsageCount(promo.getUsageCount() + 1);

                        // Nếu đạt limit thì có thể tự động chuyển status (Optional)
                        if (promo.getUsageLimit() != null && promo.getUsageCount() >= promo.getUsageLimit()) {
                            // promo.setStatus(PromotionStatus.EXPIRED); // Tuỳ logic business
                        }
                        promotionRepo.save(promo);
                    });
        }
        // Validate: Chỉ thanh toán được khi đơn đang CHỜ
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return ApiResponse.error("Đơn hàng không ở trạng thái chờ thanh toán.");
        }

        // 1. Cập nhật Payment
        payment.setPaymentMethod(paymentMethod);
        payment.setTotalAmount(booking.getTotalPrice());
        payment.setPaymentStatus(PaymentStatus.APPROVED); // ✅ Tiền đã về Admin
        payment.setPaymentDate(LocalDateTime.now());
        payment.setConfirmedDate(LocalDateTime.now());

        String trxRef = "TRX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setTransactionReference(trxRef);

        payment.setNote(note);
        paymentRepo.save(payment);

        // 2. Cập nhật Booking -> CONFIRMED (Chốt phòng)
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking);

        // 3. Gửi Email xác nhận
        sendConfirmationEmail(booking, trxRef);

        return ApiResponse.success("Thanh toán thành công!", new PaymentResponseDTO(payment, null));
    }

    // =================================================================
    // 2. REQUEST REFUND (Khách gửi yêu cầu hoàn tiền)
    // =================================================================
    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, RefundSubmitDTO req) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Validate Logic chặt chẽ
        if (booking.getStatus() != BookingStatus.CANCELLED) {
            return ApiResponse.error("Bạn phải Hủy phòng trước khi yêu cầu hoàn tiền.");
        }
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) {
            return ApiResponse.error("Đơn hàng chưa thanh toán thành công, không thể hoàn tiền.");
        }
        if (refundRepo.existsByBooking(booking)) {
            return ApiResponse.error("Yêu cầu hoàn tiền đang chờ xử lý, vui lòng đợi.");
        }

        // Tạo Refund Request (Entity riêng)
        RefundRequest refund = new RefundRequest();
        refund.setBooking(booking); // Link với Booking
        refund.setAmount(booking.getRefundAmount()); // Số tiền được hoàn (đã trừ phạt)
        refund.setReason(req.getReason());

        // Thông tin nhận tiền
        refund.setBankName(req.getBankName());
        refund.setAccountNumber(req.getAccountNumber());
        refund.setAccountHolder(req.getAccountHolder());

        refund.setStatus(RefundRequestStatus.PENDING); // Enum: PENDING
        refund.setRequestDate(LocalDateTime.now());

        refundRepo.save(refund);

        // Đánh dấu Payment đang có khiếu nại/yêu cầu
        payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);
        paymentRepo.save(payment);

        return ApiResponse.success("Gửi yêu cầu thành công", new PaymentResponseDTO(payment, refund));
    }

    // =================================================================
    // 3. ADMIN PROCESS REFUND (Admin Duyệt/Từ chối)
    // =================================================================
    @Transactional
    public ApiResponse<?> processRefund(int refundRequestId, boolean isApproved, String adminNote) {
        RefundRequest refund = refundRepo.findById(refundRequestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));

        // Lấy Payment qua Booking (vì RefundRequest link với Booking)
        Booking booking = refund.getBooking();
        Payment payment = paymentRepo.findByBooking_BookingId(booking.getBookingId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (refund.getStatus() != RefundRequestStatus.PENDING) {
            return ApiResponse.error("Yêu cầu này đã được xử lý trước đó.");
        }

        if (isApproved) {
            // --- DUYỆT ---
            refund.setStatus(RefundRequestStatus.APPROVED);

            // Update trạng thái dòng tiền -> Đã chi tiền ra
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(refund.getAmount());

            // Note: Ở đây có thể gọi API ngân hàng để chuyển tiền thật

        } else {
            // --- TỪ CHỐI ---
            refund.setStatus(RefundRequestStatus.REJECTED);

            // Update trạng thái dòng tiền -> Quay về APPROVED (Admin giữ tiền phạt)
            payment.setPaymentStatus(PaymentStatus.APPROVED);
        }

        // Lưu log Admin
        refund.setResolveDate(LocalDateTime.now());
        refund.setAdminNote(adminNote);
        payment.setNote(payment.getNote() + " | Refund Processed: " + (isApproved ? "APPROVED" : "REJECTED") + " - " + adminNote);

        refundRepo.save(refund);
        paymentRepo.save(payment);

        return ApiResponse.success(isApproved ? "Đã duyệt hoàn tiền" : "Đã từ chối hoàn tiền", null);
    }

    // =================================================================
    // 4. LỊCH SỬ GIAO DỊCH
    // =================================================================
    public List<PaymentResponseDTO> getUserTransactionHistory(String userId) {
        List<Payment> payments = paymentRepo.findByBooking_User_UserIdOrderByPaymentDateDesc(userId);
        return payments.stream()
                .map(p -> {
                    RefundRequest rr = refundRepo.findByBooking(p.getBooking()).orElse(null);
                    return new PaymentResponseDTO(p, rr);
                })
                .collect(Collectors.toList());
    }

    public List<PaymentResponseDTO> getAllTransactions() {
        return paymentRepo.findAllByOrderByPaymentDateDesc().stream()
                .map(p -> {
                    RefundRequest rr = refundRepo.findByBooking(p.getBooking()).orElse(null);
                    return new PaymentResponseDTO(p, rr);
                })
                .collect(Collectors.toList());
    }

    // Helper gửi mail
    private void sendConfirmationEmail(Booking booking, String trxRef) {
        try {
            Context context = new Context();
            context.setVariable("username", booking.getUser().getFullName());
            context.setVariable("bookingId", booking.getBookingId());
            context.setVariable("checkInDate", booking.getCheckInDate());
            context.setVariable("totalPrice", booking.getTotalPrice());
            context.setVariable("transactionRef", trxRef);

            emailService.sendHtmlEmail(
                    booking.getUser().getEmail(),
                    "Xác nhận đặt phòng thành công #" + booking.getBookingId(),
                    "email/booking-confirmation",
                    context
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
        }
    }
}