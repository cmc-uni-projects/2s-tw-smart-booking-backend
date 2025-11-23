package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.RefundSubmitDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.entity.RefundRequest;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.PaymentStatus;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PaymentRepository;
import com.example.smart_booking_system.repository.RefundRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final RefundRequestRepository refundRepo;
    private final EmailService emailService;

    // 1. SUBMIT PAYMENT (Khách thanh toán)
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, String note) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                booking.getStatus() != BookingStatus.CANCELLED) {
            return ApiResponse.error("Không thể thanh toán cho trạng thái: " + booking.getStatus());
        }

        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(new Payment());
        payment.setBooking(booking);
        payment.setPaymentMethod("AUTO_PAYMENT");
        payment.setAmount(booking.getTotalPrice());
        payment.setPaymentEvidenceUrl(null);
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setConfirmedDate(LocalDateTime.now());
        payment.setNote(note + " | Auto Confirmed (Instant Payment)");
        payment.setPaymentDate(LocalDateTime.now());

        if (payment.getRefundedAmount() == null) {
            payment.setRefundedAmount(BigDecimal.ZERO);
        }

        paymentRepo.save(payment);

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepo.save(booking);

        // ✅ KHÔI PHỤC LOGIC GỬI EMAIL
        try {
            Context context = new Context();
            // Bạn có thể thay đổi URL này thành URL frontend thực tế của bạn
            String bookingUrl = "http://localhost:5173/bookings/" + booking.getBookingId();

            context.setVariable("username", booking.getUser().getFullName());
            context.setVariable("bookingId", booking.getBookingId());
            context.setVariable("bookingUrl", bookingUrl);
            context.setVariable("checkInDate", booking.getCheckInDate());
            context.setVariable("checkOutDate", booking.getCheckOutDate());

            emailService.sendHtmlEmail(
                    booking.getUser().getEmail(),
                    "✅ Xác nhận đặt phòng thành công - Booking #" + booking.getBookingId(),
                    "email/booking-confirmation",
                    context
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail confirmation: " + e.getMessage());
        }

        return ApiResponse.success("Thanh toán thành công!", new PaymentResponseDTO(payment));
    }

    // 2. GET USER HISTORY (Lấy lịch sử giao dịch của User)
    public List<PaymentResponseDTO> getUserTransactionHistory(String userId) {
        List<Payment> payments = paymentRepo.findByBooking_User_UserIdOrderByPaymentDateDesc(userId);
        return payments.stream()
                .map(p -> {
                    // Tìm Refund Request nếu có để hiển thị chi tiết
                    RefundRequest rr = refundRepo.findByPayment_PaymentId(p.getPaymentId()).orElse(null);
                    return new PaymentResponseDTO(p, rr);
                })
                .collect(Collectors.toList());
    }

    // 3. REQUEST REFUND (Khách gửi yêu cầu hoàn tiền)
    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, RefundSubmitDTO req) {
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) {
            return ApiResponse.error("Chỉ được hoàn tiền khi đã thanh toán thành công.");
        }

        // Tạo bản ghi RefundRequest mới
        RefundRequest refund = new RefundRequest();
        refund.setPayment(payment);
        refund.setBankName(req.getBankName());
        refund.setAccountNumber(req.getAccountNumber());
        refund.setAccountHolder(req.getAccountHolder());
        refund.setReason(req.getReason());

        refundRepo.save(refund);

        // Update Payment Status -> REFUND_REQUESTED
        payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);
        paymentRepo.save(payment);

        return ApiResponse.success("Gửi yêu cầu thành công", null);
    }

    // 4. ADMIN PROCESS REFUND (Admin xác nhận hoàn tiền)
    @Transactional
    public ApiResponse<?> processRefund(int bookingId) {
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Tìm Refund Request tương ứng
        RefundRequest refund = refundRepo.findByPayment_PaymentId(payment.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));

        // Update Payment
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        String oldNote = payment.getNote() != null ? payment.getNote() : "";
        payment.setNote(oldNote + " | Admin đã hoàn tiền vào " + LocalDateTime.now());

        // Update RefundRequest Status
        refund.setStatus(PaymentStatus.REFUNDED);
        refund.setProcessDate(LocalDateTime.now());

        paymentRepo.save(payment);
        refundRepo.save(refund);

        return ApiResponse.success("Hoàn tiền thành công", null);
    }

    // 5. GET ALL TRANSACTIONS (Cho Admin)
    public List<PaymentResponseDTO> getAllTransactions() {
        return paymentRepo.findAllByOrderByPaymentDateDesc().stream()
                .map(p -> {
                    // Tìm Refund Request tương ứng nếu có
                    RefundRequest rr = refundRepo.findByPayment_PaymentId(p.getPaymentId()).orElse(null);
                    // Gọi constructor mới để map cả thông tin refund
                    return new PaymentResponseDTO(p, rr);
                })
                .collect(Collectors.toList());
    }
}