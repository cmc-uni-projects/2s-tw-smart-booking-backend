package com.example.smart_booking_system.service;

import org.thymeleaf.context.Context;
public interface EmailService {

    void sendVerificationEmail(String toEmail, String fullName, String verificationToken);

    void sendResetPasswordEmail(String toEmail, String fullName, String resetToken);

    void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId);

    void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId);

    void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason);

    void sendHtmlEmail(String to, String subject, String templateName, Context context);

    void sendPaymentReminderEmail(String toEmail, String fullName, String bookingId, String totalPrice);

    void sendCancellationRequestReceivedEmail(String toEmail, String fullName, String bookingId);

    void sendCancellationSuccessEmail(String toEmail, String fullName, String bookingId, String refundAmount, String penaltyAmount);
    void sendRefundProcessedEmail(String toEmail, String fullName, String bookingId, boolean isApproved, String refundAmount, String reason);
}
