package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.PaymentStatus;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import com.example.smart_booking_system.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;

    private final EmailService emailService;

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


        try {
            Context context = new Context();
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

        return ApiResponse.success("Thanh toán thành công! Đơn đặt phòng đã được xác nhận.", new PaymentResponseDTO(payment));
    }
    public List<PaymentResponseDTO> getUserTransactionHistory(String userId) {
        List<Payment> payments = paymentRepo.findByBooking_User_UserIdOrderByPaymentDateDesc(userId);
        return payments.stream()
                .map(PaymentResponseDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse<?> processRefund(int bookingId) {
        // 1. Tìm Payment
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán cho Booking ID: " + bookingId));

        // 2. Validate
        if (payment.getBooking().getStatus() != BookingStatus.CANCELLED) {
            return ApiResponse.error("Chỉ có thể hoàn tiền cho đơn đã hủy (CANCELLED).");
        }
        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return ApiResponse.error("Đơn này đã được hoàn tiền trước đó.");
        }
        if (payment.getRefundedAmount() == null || payment.getRefundedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("Không có số tiền cần hoàn (Refund Amount = 0).");
        }

        // 3. Cập nhật trạng thái
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.setNote(payment.getNote() + " | [Admin] Đã xác nhận hoàn tiền: " + payment.getRefundedAmount() + " VND");

        paymentRepo.save(payment);

        // 4. Gửi Email thông báo hoàn tiền (Optional)
        try {
            Context context = new Context();
            context.setVariable("username", payment.getBooking().getUser().getFullName());
            context.setVariable("bookingId", bookingId);
            context.setVariable("refundAmount", payment.getRefundedAmount());

            // Gửi mail (giả sử bạn có template email-refund-success.html)
            // emailService.sendHtmlEmail(payment.getBooking().getUser().getEmail(), "💰 Thông báo hoàn tiền thành công", "email/refund-success", context);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail hoàn tiền: " + e.getMessage());
        }

        return ApiResponse.success("Đã xác nhận hoàn tiền thành công.", new PaymentResponseDTO(payment));
    }
    @Transactional
    public ApiResponse<?> requestRefundByUser(int bookingId, String reason) { // ✅ Thêm tham số String reason
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin thanh toán."));

        // Validate: Chỉ được yêu cầu khi Booking đã HỦY và Payment đã APPROVED
        if (payment.getBooking().getStatus() != BookingStatus.CANCELLED) {
            return ApiResponse.error("Bạn phải hủy phòng trước khi yêu cầu hoàn tiền.");
        }
        if (payment.getPaymentStatus() != PaymentStatus.APPROVED) {
            return ApiResponse.error("Trạng thái thanh toán không hợp lệ để hoàn tiền (Phải là APPROVED).");
        }

        // Cập nhật trạng thái
        payment.setPaymentStatus(PaymentStatus.REFUND_REQUESTED);

        String log = " | Khách yêu cầu hoàn tiền (" + LocalDateTime.now() + "): " + reason;
        payment.setNote(payment.getNote() + log); // ✅ Ghi lý do vào note

        paymentRepo.save(payment);

        return ApiResponse.success("Đã gửi yêu cầu hoàn tiền. Vui lòng chờ Admin xử lý.", new PaymentResponseDTO(payment));
    }

    public List<PaymentResponseDTO> getAllTransactions() {
        return paymentRepo.findAllByOrderByPaymentDateDesc().stream()
                .map(PaymentResponseDTO::new)
                .collect(Collectors.toList());
    }

}