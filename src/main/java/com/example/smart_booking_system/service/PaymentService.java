package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.PaymentResponseDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

}