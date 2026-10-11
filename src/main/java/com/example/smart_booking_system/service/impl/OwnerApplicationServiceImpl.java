package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.i18n.UserLocales;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.InternalServerException;
import lombok.extern.slf4j.Slf4j;
import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.enums.NotificationType;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.dto.request.application.OwnerApplicationSubmitDTO;
import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.entity.OwnerApplication;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.ApplicationStatus;
import com.example.smart_booking_system.repository.OwnerApplicationRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.service.OwnerApplicationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.smart_booking_system.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.thymeleaf.context.Context;
import java.time.format.DateTimeFormatter;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.entity.Role;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final Messages messages;
    private final UserLocales userLocales;

    private final OwnerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;


    // 🔥 Dùng để upload + generate signed URL
    private final FileStorageService fileStorageService;

    @Override
    public OwnerApplicationDTO submitApplication(OwnerApplicationSubmitDTO submitDTO, String applicantUsername) {

        User applicant = userRepository.findByEmail(applicantUsername)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", applicantUsername));

        OwnerApplication application = new OwnerApplication();
        application.setUserId(applicant);

        application.setPermanentAddress(submitDTO.getPermanentAddress());
        application.setHometownAddress(submitDTO.getHometownAddress());
        application.setBusinessLicenseNumber(submitDTO.getBusinessLicenseNumber());
        application.setPersonalDob(submitDTO.getPersonalDob());
        application.setPersonalIdCard(submitDTO.getPersonalIdCard());

        // ============================
        // 📌 UPLOAD ẢNH (Giữ nguyên logic upload)
        // ============================
        String frontUrl = fileStorageService.storeImageFile(
                submitDTO.getCardFrontImage(),
                "owner-applications/card-front"
        );

        String backUrl = fileStorageService.storeImageFile(
                submitDTO.getCardBackImage(),
                "owner-applications/card-back"
        );

        String licenseUrl = fileStorageService.storeImageFile(
                submitDTO.getBusinessLicenseImage(),
                "owner-applications/business-license"
        );

        application.setCardFrontImage(frontUrl);
        application.setCardBackImage(backUrl);
        application.setBusinessLicenseImage(licenseUrl);

        OwnerApplication savedApp = applicationRepository.save(application);

        try {
            notificationService.sendToAllAdmins(NotificationType.ADMIN_NEW_OWNER_REGISTRATION, savedApp.getId().toString(), "notification.admin.ownerApplication", applicant.getFullName());
        } catch (Exception e) {
            log.warn("Failed to notify admins about owner application: {}", e.getMessage());
        }

        // Email giữ nguyên
        try {
            String subject = "email.subject.ownerApplicationSubmitted";
            String templateName = "email/application-submitted-confirmation";

            Context context = new Context();
            context.setVariable("applicantName", applicant.getFullName());
            context.setVariable("applicationId", savedApp.getId());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            context.setVariable("submittedAt", savedApp.getCreatedAt().format(formatter));

            emailService.sendHtmlEmail(applicant.getEmail(), subject, templateName, context);

        } catch (Exception e) {
            log.warn("Failed to send owner application confirmation email: {}", e.getMessage());
        }

        return convertToDTO(savedApp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerApplicationDTO> getPendingOwnerApplications() {
        return this.getApplicationsByStatus(ApplicationStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status) {
        return applicationRepository.findByStatusWithApplicant(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OwnerApplicationDTO reviewApplication(Long applicationId, OwnerApplicationReviewDTO reviewDTO, String adminUsername) {

        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", adminUsername));

        OwnerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("error.ownerApplication.notFound", applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("error.ownerApplication.alreadyProcessed");
        }

        ApplicationStatus newStatus = reviewDTO.getStatus();
        if (newStatus == null)
            throw new BadRequestException("error.ownerApplication.statusRequired");

        if (newStatus != ApplicationStatus.APPROVED && newStatus != ApplicationStatus.REJECTED)
            throw new BadRequestException("error.ownerApplication.invalidStatus");

        application.setAdminReason(reviewDTO.getReason());
        application.setStatus(newStatus);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);

        User applicant = application.getUserId();

        if (newStatus == ApplicationStatus.APPROVED) {
            Role ownerRole = roleRepository.findByRoleName("OWNER")
                    .orElseThrow(() -> new InternalServerException("error.role.ownerMissing"));

            applicant.addRole(ownerRole);
            userRepository.save(applicant);
        }

        OwnerApplication savedApp = applicationRepository.save(application);

        // [BỔ SUNG] Gửi thông báo In-App cho User (Applicant)
        if (applicant != null) {
            // 1. Gửi email (code cũ)
            sendReviewNotificationEmail(applicant, savedApp);

            // 2. Gửi thông báo hệ thống (code mới)
            try {
                if (newStatus == ApplicationStatus.APPROVED) {
                    notificationService.sendNotification(applicant.getUserId(), NotificationType.APPROVAL, savedApp.getId().toString(), "notification.ownerApplication.approved");
                } else if (newStatus == ApplicationStatus.REJECTED) {
                    notificationService.sendNotification(applicant.getUserId(), NotificationType.REJECTION, savedApp.getId().toString(), "notification.ownerApplication.rejected", reviewDTO.getReason() != null ? reviewDTO.getReason() : messages.t(userLocales.of(applicant), "common.noReason"));
                }
            } catch (Exception e) {
                log.warn("Failed to notify applicant about review result: {}", e.getMessage());
            }
        }

        return convertToDTO(savedApp);
    }

    // ============================================================
    // 🔥 FIX QUAN TRỌNG: Trả về Signed URL cho 3 ảnh
    // ============================================================
    private OwnerApplicationDTO convertToDTO(OwnerApplication app) {

        OwnerApplicationDTO dto = new OwnerApplicationDTO();
        dto.setId(app.getId());
        dto.setStatus(app.getStatus());
        dto.setPermanentAddress(app.getPermanentAddress());
        dto.setHometownAddress(app.getHometownAddress());

        // ============================
        // 🔥 Ảnh trả về signed URL để FE load được
        // ============================
        dto.setCardFrontImage(
                fileStorageService.generateSignedUrl(app.getCardFrontImage())
        );

        dto.setCardBackImage(
                fileStorageService.generateSignedUrl(app.getCardBackImage())
        );

        dto.setBusinessLicenseImage(
                fileStorageService.generateSignedUrl(app.getBusinessLicenseImage())
        );

        dto.setBusinessLicenseNumber(app.getBusinessLicenseNumber());
        dto.setApplicantDob(app.getPersonalDob());
        dto.setPersonalIdCard(app.getPersonalIdCard());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setReviewedAt(app.getReviewedAt());
        dto.setAdminReason(app.getAdminReason());

        if (app.getUserId() != null) {
            User applicant = app.getUserId();

            dto.setApplicantId(applicant.getUserId());
            dto.setApplicantFullName(applicant.getFullName());
            dto.setApplicantEmail(applicant.getEmail());
            dto.setApplicantPhoneNumber(applicant.getPhoneNumber());

            if (applicant.getUserDetail() != null &&
                    applicant.getUserDetail().getProfilePhotoUrl() != null) {

                dto.setApplicantAvatar(
                        fileStorageService.generateSignedUrl(
                                applicant.getUserDetail().getProfilePhotoUrl()
                        )
                );
            }
        }

        if (app.getReviewedBy() != null) {
            dto.setReviewedByAdminName(app.getReviewedBy().getFullName());
        }

        return dto;
    }

    private void sendReviewNotificationEmail(User applicant, OwnerApplication application) {

        String applicantEmail = applicant.getEmail();
        String applicantName = applicant.getFullName();
        String adminReason = application.getAdminReason();
        ApplicationStatus status = application.getStatus();

        Context context = new Context();
        context.setVariable("applicantName", applicantName);
        context.setVariable("reason", adminReason != null && !adminReason.isEmpty() ? adminReason : "N/A");

        String subject, templateName;

        if (status == ApplicationStatus.APPROVED) {
            subject = "email.subject.ownerApplicationApproved";
            templateName = "email/application-approved";

        } else if (status == ApplicationStatus.REJECTED) {
            subject = "email.subject.ownerApplicationRejected";
            templateName = "email/application-rejected";

        } else {
            return;
        }

        emailService.sendHtmlEmail(applicantEmail, subject, templateName, context);
    }
}
