package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Payment;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.PaymentStatus;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import com.example.smart_booking_system.dto.response.PaymentResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    // ==========================================================================
    // 1. KHÁCH HÀNG: Gửi bằng chứng thanh toán (Upload ảnh)
    // ==========================================================================
    @Transactional
    public ApiResponse<?> submitPayment(int bookingId, MultipartFile evidenceImage, String note) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Chỉ cho phép gửi thanh toán khi đơn đang Chờ thanh toán, Đã hủy (muốn khôi phục) hoặc Đang chờ duyệt (gửi lại ảnh)
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                booking.getStatus() != BookingStatus.CANCELLED &&
                booking.getStatus() != BookingStatus.AWAITING_CONFIRMATION) {
            return ApiResponse.error("Không thể gửi thanh toán cho trạng thái: " + booking.getStatus());
        }

        // 1. Upload ảnh lên server
        String evidenceUrl = fileStorageService.storeImageFile(evidenceImage, "payments");

        // 2. Lưu/Cập nhật thông tin Payment
        Payment payment = paymentRepo.findByBooking_BookingId(bookingId).orElse(new Payment());
        payment.setBooking(booking);
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setAmount(booking.getTotalPrice());
        payment.setPaymentEvidenceUrl(evidenceUrl);
        payment.setPaymentStatus(PaymentStatus.PENDING); // Chờ Admin duyệt
        payment.setNote(note);
        payment.setPaymentDate(LocalDateTime.now());

        paymentRepo.save(payment);

        // 3. Cập nhật trạng thái Booking -> Chờ duyệt
        booking.setStatus(BookingStatus.AWAITING_CONFIRMATION);
        bookingRepo.save(booking);

        // ✅ GỬI EMAIL 1: ĐÃ NHẬN ẢNH (Dùng file: payment-submitted.html)
        try {
            Context context = new Context();
            context.setVariable("customerName", booking.getUser().getFullName());
            context.setVariable("bookingId", bookingId);
            context.setVariable("amount", booking.getTotalPrice() + " VND");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            context.setVariable("submittedDate", LocalDateTime.now().format(formatter));

            emailService.sendHtmlEmail(
                    booking.getUser().getEmail(),
                    "Đã nhận thông tin thanh toán - Booking #" + bookingId,
                    "email/payment-submitted",
                    context
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail payment-submitted: " + e.getMessage());
        }

        return ApiResponse.success("Đã gửi thông tin thanh toán thành công", new PaymentResponseDTO(payment));
    }

    // ==========================================================================
    // 2. ADMIN: Duyệt hoặc Từ chối thanh toán
    // ==========================================================================
    @Transactional
    public ApiResponse<?> reviewPayment(int paymentId, boolean isApproved, String adminReason) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        Booking booking = payment.getBooking();
        String customerEmail = booking.getUser().getEmail();
        String customerName = booking.getUser().getFullName();

        Context context = new Context();
        context.setVariable("bookingId", booking.getBookingId()); // Biến chung

        if (isApproved) {
            // --- TRƯỜNG HỢP DUYỆT (APPROVE) ---
            payment.setPaymentStatus(PaymentStatus.APPROVED);
            payment.setConfirmedDate(LocalDateTime.now());
            payment.setNote("Admin Approved: " + adminReason);

            booking.setStatus(BookingStatus.CONFIRMED);

            // ✅ GỬI EMAIL 2: XÁC NHẬN THÀNH CÔNG (Dùng file cũ: booking-confirmation.html)
            try {
                // Các biến cần thiết cho booking-confirmation.html
                String bookingUrl = "http://localhost:5173/bookings/" + booking.getBookingId();
                context.setVariable("username", customerName);
                context.setVariable("bookingUrl", bookingUrl);
                context.setVariable("checkInDate", booking.getCheckInDate());
                context.setVariable("checkOutDate", booking.getCheckOutDate());

                emailService.sendHtmlEmail(
                        customerEmail,
                        "✅ Xác nhận đặt phòng thành công - Booking #" + booking.getBookingId(),
                        "email/booking-confirmation",
                        context
                );
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail confirmation: " + e.getMessage());
            }

        } else {
            // --- TRƯỜNG HỢP TỪ CHỐI (REJECT) ---
            payment.setPaymentStatus(PaymentStatus.REJECTED);
            payment.setNote("Admin Rejected: " + adminReason);

            booking.setStatus(BookingStatus.CANCELLED);

            // ✅ GỬI EMAIL 3: TỪ CHỐI THANH TOÁN (Dùng file: payment-rejected.html)
            try {
                context.setVariable("customerName", customerName);
                context.setVariable("reason", adminReason != null ? adminReason : "Thông tin không hợp lệ");

                emailService.sendHtmlEmail(
                        customerEmail,
                        "⚠️ Thanh toán bị từ chối - Booking #" + booking.getBookingId(),
                        "email/payment-rejected",
                        context
                );
            } catch (Exception e) {
                System.err.println("Lỗi gửi mail rejected: " + e.getMessage());
            }
        }

        paymentRepo.save(payment);
        bookingRepo.save(booking);

        String msg = isApproved ? "Đã duyệt thanh toán" : "Đã từ chối thanh toán";
        return ApiResponse.success(msg, new PaymentResponseDTO(payment));
    }
}