package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.i18n.I18nConfig;
import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.i18n.UserLocales;
import com.example.smart_booking_system.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Sends HTML emails rendered from templates/email/*.html.
 * Every email is written in the recipient's language: users.preferredLanguage of the account
 * owning the address, else the current request's language, else Vietnamese.
 * Subjects and template texts live in i18n/emails*.properties.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String LOGO_PATH = "static/images/logo-travelmate.png";

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final Messages messages;
    private final UserLocales userLocales;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private String getFrontendBaseUrl() {
        return frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
    }

    // ==================================================
    // Account
    // ==================================================
    @Override
    public void sendVerificationEmail(String toEmail, String fullName, String verificationToken) {
        String url = getFrontendBaseUrl() + "verify-email?token=" + URLEncoder.encode(verificationToken, StandardCharsets.UTF_8);
        sendOrThrow(toEmail, "email/verification-email", "email.subject.verification",
                vars("username", fullName, "verificationUrl", url));
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String fullName, String resetToken) {
        String url = getFrontendBaseUrl() + "reset-password?token=" + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
        sendOrThrow(toEmail, "email/reset-password-email", "email.subject.resetPassword",
                vars("username", fullName, "resetPasswordUrl", url));
    }

    @Override
    @Async
    public void sendAccountLockedEmail(String to, String name, String reason) {
        sendQuietly(to, "email/account-locked", "email.subject.accountLocked",
                vars("name", name, "reason", reason, "helpUrl", getFrontendBaseUrl() + "help"));
    }

    // ==================================================
    // Owner applications
    // ==================================================
    @Override
    public void sendOwnerApplicationNotification(String adminEmail, String applicantName, String applicationId) {
        sendOrThrow(adminEmail, "email/owner-application-notification", "email.subject.ownerApplicationNew",
                vars("applicantName", applicantName, "applicationId", applicationId,
                        "reviewUrl", getFrontendBaseUrl() + "admin/applications/" + applicationId));
    }

    @Override
    public void sendApplicationStatusEmail(String toEmail, String fullName, String status, String reason) {
        sendOrThrow(toEmail, "email/application-status", "email.subject.applicationStatus",
                vars("username", fullName, "status", status, "reason", reason, "loginUrl", getFrontendBaseUrl() + "login"));
    }

    // ==================================================
    // Bookings
    // ==================================================
    @Override
    public void sendBookingConfirmationEmail(String toEmail, String fullName, String bookingId) {
        sendOrThrow(toEmail, "email/booking-confirmation", "email.subject.bookingConfirmation",
                vars("username", fullName, "bookingId", bookingId, "bookingUrl", getFrontendBaseUrl() + "bookings/" + bookingId),
                bookingId);
    }

    @Override
    public void sendPaymentReminderEmail(String toEmail, String fullName, String bookingId, String totalPrice) {
        sendOrThrow(toEmail, "email/booking-pending", "email.subject.paymentReminder",
                vars("username", fullName, "bookingId", bookingId, "totalPrice", totalPrice), bookingId);
    }

    @Override
    public void sendCancellationRequestReceivedEmail(String toEmail, String fullName, String bookingId, BigDecimal totalPrice, BigDecimal penalty, BigDecimal refund) {
        sendQuietly(toEmail, "email/cancellation-request", "email.subject.cancellationRequest",
                vars("username", fullName, "bookingId", bookingId,
                        "totalPrice", String.format("%,.0f", totalPrice),
                        "penaltyAmount", String.format("%,.0f", penalty),
                        "refundAmount", String.format("%,.0f", refund)),
                bookingId);
    }

    @Override
    public void sendCancellationSuccessEmail(String toEmail, String fullName, String bookingId, String refundAmount, String penaltyAmount) {
        sendQuietly(toEmail, "email/cancellation-success", "email.subject.cancellationSuccess",
                vars("username", fullName, "bookingId", bookingId, "refundAmount", refundAmount, "penaltyAmount", penaltyAmount),
                bookingId);
    }

    @Override
    public void sendRefundRejectionEmail(String toEmail, String fullName, String bookingId, String rejectionReason) {
        sendQuietly(toEmail, "email/refund-rejected", "email.subject.refundRejected",
                vars("username", fullName, "bookingId", bookingId, "reason", rejectionReason,
                        "contactUrl", getFrontendBaseUrl() + "contact"),
                bookingId);
    }

    @Override
    public void sendCheckinReminderEmail(String toEmail, String fullName, String bookingId, String propertyName, String checkInDate) {
        sendQuietly(toEmail, "email/checkin-reminder", "email.subject.checkinReminder",
                vars("username", fullName, "bookingId", bookingId, "propertyName", propertyName,
                        "checkInDate", checkInDate, "bookingUrl", getFrontendBaseUrl() + "customer/bookings"));
    }

    @Override
    public void sendThankYouEmail(String toEmail, String fullName, String bookingId, String propertyName) {
        sendQuietly(toEmail, "email/checkout-thankyou", "email.subject.thankYou",
                vars("username", fullName, "propertyName", propertyName, "bookingId", bookingId,
                        "reviewUrl", getFrontendBaseUrl() + "customer/bookings"),
                propertyName);
    }

    // ==================================================
    // Property / room status (to the owner)
    // ==================================================
    @Override
    public void sendPropertySuspensionEmail(String to, String ownerName, String propertyName, String reason) {
        sendQuietly(to, "email/property-suspended", "email.subject.propertySuspended",
                vars("ownerName", ownerName, "propertyName", propertyName, "reason", reason));
    }

    @Override
    public void sendRoomSuspensionEmail(String to, String ownerName, String propertyName, String roomName, String reason) {
        sendQuietly(to, "email/room-suspended", "email.subject.roomSuspended",
                vars("ownerName", ownerName, "propertyName", propertyName, "roomName", roomName, "reason", reason));
    }

    @Override
    public void sendPropertyReactivationEmail(String to, String ownerName, String propertyName) {
        sendQuietly(to, "email/property-reactivated", "email.subject.propertyReactivated",
                vars("ownerName", ownerName, "propertyName", propertyName));
    }

    @Override
    public void sendRoomReactivationEmail(String to, String ownerName, String propertyName, String roomName) {
        sendQuietly(to, "email/room-reactivated", "email.subject.roomReactivated",
                vars("ownerName", ownerName, "propertyName", propertyName, "roomName", roomName));
    }

    /**
     * Generic async send. {@code subjectKey} is a key in i18n/emails*.properties;
     * the context's variables are copied into a context carrying the recipient's locale.
     */
    @Override
    @Async
    public void sendHtmlEmail(String to, String subjectKey, String templateName, Context context) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (String name : context.getVariableNames()) {
            variables.put(name, context.getVariable(name));
        }
        sendQuietly(to, templateName, subjectKey, variables);
    }

    // ==================================================
    // Internals
    // ==================================================

    /** Send; on failure log and rethrow (callers that must know, e.g. registration). */
    private void sendOrThrow(String to, String template, String subjectKey, Map<String, Object> variables, Object... subjectArgs) {
        try {
            send(to, template, subjectKey, variables, subjectArgs);
        } catch (Exception e) {
            log.error("Email '{}' to {} failed: {}", template, to, e.getMessage());
            throw new RuntimeException("Failed to send email " + template, e);
        }
    }

    /** Send; on failure only log (notification-style emails must not break the business action). */
    private void sendQuietly(String to, String template, String subjectKey, Map<String, Object> variables, Object... subjectArgs) {
        try {
            send(to, template, subjectKey, variables, subjectArgs);
        } catch (Exception e) {
            log.error("Email '{}' to {} failed: {}", template, to, e.getMessage());
        }
    }

    private void send(String to, String template, String subjectKey, Map<String, Object> variables, Object... subjectArgs)
            throws MessagingException {
        Locale locale = recipientLocale(to);
        Context context = new Context(locale, variables);
        String html = templateEngine.process(template, context);
        String subject = messages.t(locale, subjectKey, subjectArgs);
        sendHtmlEmailInternal(to, subject, html);
        log.info("Email '{}' sent to {} ({})", template, to, locale.getLanguage());
    }

    private Locale recipientLocale(String email) {
        Optional<Locale> accountLocale = userLocales.ofAccount(email);
        if (accountLocale.isPresent()) {
            return accountLocale.get();
        }
        // Not an account address (e.g. a guest's contact email): use the language of the current request, if any.
        if (RequestContextHolder.getRequestAttributes() != null) {
            return I18nConfig.toSupported(LocaleContextHolder.getLocale().getLanguage());
        }
        return I18nConfig.VIETNAMESE;
    }

    private static Map<String, Object> vars(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private void sendHtmlEmailInternal(String to, String subject, String htmlContent) throws MessagingException {
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

        // Inline logo referenced as cid:logoImage. The explicit content type is required by desktop Outlook.
        ClassPathResource logoResource = new ClassPathResource(LOGO_PATH);
        if (logoResource.exists()) {
            helper.addInline("logoImage", logoResource, "image/png");
        } else {
            log.warn("Email logo not found at {}", LOGO_PATH);
        }

        mailSender.send(message);
    }
}
