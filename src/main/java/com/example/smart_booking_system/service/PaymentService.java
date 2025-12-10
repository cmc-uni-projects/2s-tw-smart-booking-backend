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
    private final NotificationService notificationService;

    // =================================================================
    // 1. SUBMIT PAYMENT (Khách thanh toán thành công -> Chốt đơn)
    // =================================================================
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, String note, String paymentMethod) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment info not found"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return ApiResponse.error("Đơn hàng không ở trạng thái chờ thanh toán.");
        }

        // TĂNG USAGE COUNT CHO CẢ 2 MÃ
        // 1. Tăng count mã Owner
        if (booking.getPromotionCode() != null) {
            promotionRepo.findValidPromotion(booking.getPromotionCode(), LocalDateTime.now())
                    .ifPresent(promo -> {
                        promo.setUsageCount(promo.getUsageCount() + 1);
                        promotionRepo.save(promo);
                    });
        }

        // 2. Tăng count mã Admin
        if (booking.getAdminPromotionCode() != null) {
            promotionRepo.findValidPromotion(booking.getAdminPromotionCode(), LocalDateTime.now())
                    .ifPresent(promo -> {
                        promo.setUsageCount(promo.getUsageCount() + 1);
                        promotionRepo.save(promo);
                    });
        }

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

        sendConfirmationEmail(booking, trxRef);
        try {
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(
                        owner,
                        "Thanh toán thành công #" + booking.getBookingId(),
                        "Khách hàng " + booking.getCustomerName() + " đã thanh toán " + String.format("%,.0f", booking.getTotalPrice()) + " VNĐ. Đơn hàng đã được xác nhận!",
                        NotificationType.SUCCESS, // Màu xanh
                        String.valueOf(booking.getBookingId())
                );
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo thanh toán: " + e.getMessage());
        }
        return ApiResponse.success("Thanh toán thành công!", new PaymentResponseDTO(payment, null));
    }

    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, RefundSubmitDTO req) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new RuntimeException("Payment not found"));

        if (booking.getStatus() != BookingStatus.CANCELLED) return ApiResponse.error("Phải Hủy phòng trước khi yêu cầu.");
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) return ApiResponse.error("Chưa thanh toán thành công.");
        if (refundRepo.existsByBooking(booking)) return ApiResponse.error("Đã có yêu cầu đang chờ xử lý.");

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

        return ApiResponse.success("Gửi yêu cầu thành công", new PaymentResponseDTO(payment, refund));
    }

    // ✅ SỬA: Hàm xử lý hoàn tiền (Admin duyệt)
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

        if (isApproved) {
            refund.setStatus(RefundRequestStatus.APPROVED);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(refund.getAmount());
        } else {
            refund.setStatus(RefundRequestStatus.REJECTED);
            payment.setPaymentStatus(PaymentStatus.APPROVED);
        }

        refund.setResolveDate(LocalDateTime.now());
        refund.setAdminNote(adminNote);
        refundRepo.save(refund);
        paymentRepo.save(payment);

        // ✅ LOGIC GỬI EMAIL & THÔNG BÁO (Chỉ khi duyệt)
        try {
            if (isApproved) {
                // 1. Gửi Email cho khách (Giữ nguyên)
                String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

                emailService.sendCancellationSuccessEmail(
                        emailTo,
                        nameTo,
                        String.valueOf(booking.getBookingId()),
                        String.format("%,.0f", refund.getAmount()), // Số tiền hoàn
                        String.format("%,.0f", booking.getPenaltyAmount()) // Phí phạt
                );

                // 2. 🔥 [NEW] GỬI THÔNG BÁO CHO OWNER: ĐÃ HOÀN TIỀN
                User owner = booking.getProperty().getOwner();
                if (owner != null) {
                    notificationService.sendNotification(
                            owner,
                            "Đã hoàn tiền booking #" + booking.getBookingId(),
                            "Yêu cầu hoàn tiền đã được Admin chấp thuận. Số tiền hoàn: " + String.format("%,.0f", refund.getAmount()) + " VNĐ.",
                            NotificationType.WARNING, // Màu vàng (tiền đi ra/cảnh báo thay đổi số dư)
                            String.valueOf(booking.getBookingId())
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi email/thông báo hoàn tiền: " + e.getMessage());
        }

        return ApiResponse.success(isApproved ? "Đã duyệt hoàn tiền" : "Đã từ chối hoàn tiền", null);
    }

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
}