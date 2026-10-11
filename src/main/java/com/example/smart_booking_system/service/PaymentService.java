package com.example.smart_booking_system.service;

import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.*;
import com.example.smart_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final Messages messages;

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RefundRequestRepository refundRepo;
    private final EmailService emailService;
    private final PromotionRepository promotionRepo;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    // =================================================================
    // 1. SUBMIT PAYMENT (Khách thanh toán thành công -> Chốt đơn)
    // =================================================================
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, String note, String paymentMethod) {
        // 1. Kiểm tra dữ liệu
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("error.booking.notFound"));

        entityManager.refresh(booking);

        // [DEBUG] In ra log để kiểm tra giá lúc bắt đầu thanh toán
        log.debug("Submitting payment for booking {} (price in DB {})", bookingId, booking.getTotalPrice());

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("error.payment.notFound"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            return ApiResponse.error(messages.t("error.payment.notPending"));
        }

        // 2. Xử lý logic khuyến mãi (Promotion Count)
        // Tăng count mã Owner
        if (booking.getPromotionCode() != null) {
            int updatedRows = promotionRepo.incrementUsageCountIfAvailable(booking.getPromotionCode());
            if (updatedRows == 0) throw new BadRequestException("error.promotion.ownerUnavailable", booking.getPromotionCode());
        }
        // Tăng count mã Admin
        if (booking.getAdminPromotionCode() != null) {
            int updatedRows = promotionRepo.incrementUsageCountIfAvailable(booking.getAdminPromotionCode());
            if (updatedRows == 0) throw new BadRequestException("error.promotion.adminUnavailable", booking.getAdminPromotionCode());
        }

        // 3. Cập nhật thông tin thanh toán
        payment.setPaymentMethod(paymentMethod);

        // 🔥 Đảm bảo lấy giá từ Booking (lúc này chắc chắn đúng nhờ saveAndFlush bên trên nếu có)
        payment.setTotalAmount(booking.getTotalPrice());

        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setConfirmedDate(LocalDateTime.now()); // Set thêm ngày confirm nếu cần

        String trxRef = "TRX_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        payment.setTransactionReference(trxRef);
        payment.setNote(note);
        paymentRepo.save(payment);

        // 4. Cập nhật trạng thái Booking
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking); // Lúc này save chỉ update status, giá giữ nguyên

        // 5. Gửi Email xác nhận
        sendConfirmationEmail(booking, trxRef);

        // 6. [LOGIC MỚI] Gửi thông báo (Notification)
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String priceFormatted = String.format("%,.0f", booking.getTotalPrice());

            // --- A. Gửi cho OWNER (Dùng Type: BOOKING_RECEIVED) ---
            User owner = booking.getProperty().getOwner();
            if (owner != null) {
                notificationService.sendNotification(owner.getUserId(), NotificationType.BOOKING_RECEIVED, relatedId, "notification.payment.receivedByOwner", relatedId, booking.getCustomerName(), booking.getTotalPrice());
            }

            // --- B. [BỔ SUNG] Gửi cho CUSTOMER (Dùng Type: PAYMENT_SUCCESS) ---
            // Khách hàng cũng cần biết mình đã thanh toán thành công
            User customer = booking.getUser();
            if (customer != null) {
                notificationService.sendNotification(customer.getUserId(), NotificationType.PAYMENT_SUCCESS, relatedId, "notification.payment.success", booking.getTotalPrice(), booking.getProperty().getPropertyName());
            }

        } catch (Exception e) {
            // Log lỗi notification không được làm ảnh hưởng transaction chính
            log.warn("Failed to send payment notifications (booking {}): {}", booking.getBookingId(), e.getMessage(), e);
        }

        return ApiResponse.success(messages.t("api.payment.success"), new PaymentResponseDTO(payment, null));
    }

    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, RefundSubmitDTO req) {
        Booking booking = bookingRepo.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("error.booking.notFound"));
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElseThrow(() -> new ResourceNotFoundException("error.payment.notFound"));

        if (booking.getStatus() != BookingStatus.CANCELLED) return ApiResponse.error(messages.t("error.refund.cancelFirst"));
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) return ApiResponse.error(messages.t("error.refund.notPaid"));
        if (refundRepo.existsByBooking(booking)) return ApiResponse.error(messages.t("error.refund.alreadyRequested"));

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

        return ApiResponse.success(messages.t("api.refund.requested"), new PaymentResponseDTO(payment, refund));
    }

    // ✅ SỬA: Hàm xử lý hoàn tiền (Admin duyệt)
    @Transactional
    public ApiResponse<?> processRefund(int refundRequestId, boolean isApproved, String adminNote) {
        // 1. Kiểm tra dữ liệu
        RefundRequest refund = refundRepo.findById(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("error.refund.notFound"));

        Booking booking = refund.getBooking();
        Payment payment = paymentRepo.findByBooking_BookingId(booking.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("error.payment.notFound"));

        if (refund.getStatus() != RefundRequestStatus.PENDING) {
            return ApiResponse.error(messages.t("error.refund.alreadyProcessed"));
        }

        // 2. Cập nhật trạng thái
        if (isApproved) {
            refund.setStatus(RefundRequestStatus.APPROVED);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(refund.getAmount());
        } else {
            refund.setStatus(RefundRequestStatus.REJECTED);
            // Nếu từ chối hoàn tiền, trạng thái thanh toán quay về đã thanh toán (APPROVED)
            payment.setPaymentStatus(PaymentStatus.APPROVED);
        }

        refund.setResolveDate(LocalDateTime.now());
        refund.setAdminNote(adminNote);
        refundRepo.save(refund);
        paymentRepo.save(payment);

        // 3. ✅ GỬI EMAIL & THÔNG BÁO
        try {
            String relatedId = String.valueOf(booking.getBookingId());
            String amountFormatted = String.format("%,.0f", refund.getAmount());

            // --- A. TRƯỜNG HỢP DUYỆT (APPROVED) ---
            if (isApproved) {
                // 1. Gửi Email cho khách (Logic cũ của bạn)
                String emailTo = booking.getCustomerEmail() != null ? booking.getCustomerEmail() : booking.getUser().getEmail();
                String nameTo = booking.getCustomerName() != null ? booking.getCustomerName() : booking.getUser().getFullName();

                emailService.sendCancellationSuccessEmail(
                        emailTo,
                        nameTo,
                        relatedId,
                        amountFormatted, // Số tiền hoàn
                        String.format("%,.0f", booking.getPenaltyAmount()) // Phí phạt
                );

                // 2. [FIX] Gửi thông báo cho OWNER
                User owner = booking.getProperty().getOwner();
                if (owner != null) {
                    notificationService.sendNotification(owner.getUserId(), NotificationType.BOOKING_CANCELLED_BY_GUEST, relatedId, "notification.refund.approvedForOwner", relatedId, refund.getAmount());
                }

                // 3. [MỚI] Gửi thông báo cho CUSTOMER (Quan trọng)
                // Khách cần biết yêu cầu của mình đã được duyệt
                User customer = booking.getUser();
                if (customer != null) {
                    notificationService.sendNotification(customer.getUserId(), NotificationType.REFUND_PROCESSED, relatedId, "notification.refund.requestApproved", refund.getAmount(), relatedId);
                }
            }

            // --- B. TRƯỜNG HỢP TỪ CHỐI (REJECTED) ---
            else {
                // Nên báo cho khách biết tại sao bị từ chối
                User customer = booking.getUser();
                if (customer != null) {
                    notificationService.sendNotification(customer.getUserId(), NotificationType.GENERAL, relatedId, "notification.refund.requestRejected", relatedId, adminNote);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to send refund email/notifications (booking {}): {}", booking.getBookingId(), e.getMessage(), e);
        }

        return ApiResponse.success(isApproved ? messages.t("api.refund.approved") : messages.t("api.refund.rejected"), null);
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
        } catch (Exception e) { log.warn("Failed to send booking confirmation email (booking {}): {}", booking.getBookingId(), e.getMessage(), e); }
    }
}