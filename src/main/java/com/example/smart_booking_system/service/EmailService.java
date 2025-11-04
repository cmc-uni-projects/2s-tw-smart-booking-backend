package com.example.smart_booking_system.service;

public interface EmailService {

    void sendVerificationEmail(String toEmail, String fullName, String verificationToken);

    void sendResetPasswordEmail(String toEmail, String fullName, String resetToken);

    void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId);

    void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId);

    void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason);
}
