package com.example.smart_booking_system.service;

import org.thymeleaf.context.Context;

import java.math.BigDecimal;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String fullName, String verificationToken);

    void sendResetPasswordEmail(String toEmail, String fullName, String resetToken);

    void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId);

    void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId);

    void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason);

    void sendHtmlEmail(String to, String subject, String templateName, Context context);

    void sendPaymentReminderEmail(String toEmail, String fullName, String bookingId, String totalPrice);

    // Sửa dòng này
    void sendCancellationRequestReceivedEmail(String toEmail, String fullName, String bookingId, BigDecimal totalPrice, BigDecimal penalty, BigDecimal refund);

    void sendCancellationSuccessEmail(String toEmail, String fullName, String bookingId, String refundAmount, String penaltyAmount);

    void sendCheckinReminderEmail(String toEmail, String fullName, String bookingId, String propertyName, String checkInDate);
}
