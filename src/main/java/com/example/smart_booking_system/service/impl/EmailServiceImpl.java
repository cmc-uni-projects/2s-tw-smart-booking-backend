package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String verificationToken) {
        try {
            String subject = "Verify Your Email - Smart Booking";

            // ✅ Encode token để tránh lỗi khi click từ Gmail/Outlook
            String encodedToken = URLEncoder.encode(verificationToken, StandardCharsets.UTF_8);
            String verificationUrl = frontendUrl + "/verify-email?token=" + encodedToken;

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("verificationUrl", verificationUrl);

            String htmlContent = templateEngine.process("email/verification-email", context);
            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String fullName, String resetToken) {
        try {
            String subject = "Reset Your Password - Smart Booking";
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("email/reset-password-email", context);

            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email", e);
        }
    }

    @Override
    public void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId) {
        try {
            String subject = "Booking Confirmation - Smart Booking";

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("bookingId", bookingId);
            context.setVariable("bookingUrl", frontendUrl + "/bookings/" + bookingId);

            String htmlContent = templateEngine.process("email/booking-confirmation", context);

            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send booking confirmation email", e);
        }
    }

    @Override
    public void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId) {
        try {
            String subject = "New Owner Application - Smart Booking";

            Context context = new Context();
            context.setVariable("applicantName", applicantName);
            context.setVariable("applicationId", applicationId);
            context.setVariable("reviewUrl", frontendUrl + "/admin/applications/" + applicationId);

            String htmlContent = templateEngine.process("email/owner-application-notification", context);

            sendHtmlEmail(adminEmail, subject, htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send owner application notification", e);
        }
    }

    @Override
    public void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason) {
        try {
            String subject = "Owner Application Status - Smart Booking";

            Context context = new Context();
            context.setVariable("fullName", fullName);
            context.setVariable("status", status);
            context.setVariable("reason", reason);
            context.setVariable("loginUrl", frontendUrl + "/login");

            String htmlContent = templateEngine.process("email/application-status", context);

            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send application status email", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            System.err.println("Lỗi khi gửi email HTML: " + e.getMessage());
        }
    }
}

