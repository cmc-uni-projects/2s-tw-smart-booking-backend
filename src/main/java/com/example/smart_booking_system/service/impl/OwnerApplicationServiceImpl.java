package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.service.EmailService;
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
public class OwnerApplicationServiceImpl implements OwnerApplicationService {

    private final OwnerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RoleRepository roleRepository;

    @Override
    public OwnerApplicationDTO submitApplication(OwnerApplicationSubmitDTO submitDTO, String applicantUsername) {
        User applicant = userRepository.findByEmail(applicantUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + applicantUsername));

        OwnerApplication application = new OwnerApplication();
        application.setUserId(applicant);

        application.setPermanentAddress(submitDTO.getPermanentAddress());
        application.setHometownAddress(submitDTO.getHometownAddress());
        application.setCardFrontImage(submitDTO.getCardFrontImage());
        application.setCardBackImage(submitDTO.getCardBackImage());
        application.setBusinessLicenseImage(submitDTO.getBusinessLicenseImage());
        application.setBusinessLicenseNumber(submitDTO.getBusinessLicenseNumber());

        OwnerApplication savedApp = applicationRepository.save(application);

        try {
            String subject = "Xác nhận nộp đơn đăng ký làm chủ khách sạn";
            String templateName = "email/application-submitted-confirmation";

            Context context = new Context();
            context.setVariable("applicantName", applicant.getFullName());
            context.setVariable("applicationId", savedApp.getId());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
            context.setVariable("submittedAt", savedApp.getCreatedAt().format(formatter));

            emailService.sendHtmlEmail(applicant.getEmail(), subject, templateName, context);

        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận nộp đơn: " + e.getMessage());

        }

        return convertToDTO(savedApp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status) {
        List<OwnerApplication> applications = applicationRepository.findByStatusWithApplicant(status);
        return applications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OwnerApplicationDTO reviewApplication(Long applicationId, OwnerApplicationReviewDTO reviewDTO, String adminUsername) {
        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found: " + adminUsername));
        OwnerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + applicationId));
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Đơn này đã được xử lý rồi.");
        }
        ApplicationStatus newStatus = ApplicationStatus.valueOf(reviewDTO.getStatus().toUpperCase());
        application.setAdminReason(reviewDTO.getReason());
        application.setStatus(newStatus);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(admin);

        User applicant = application.getUserId();

        if (newStatus == ApplicationStatus.APPROVED) {
            if (applicant == null) throw new EntityNotFoundException("Không tìm thấy người nộp đơn.");


            Role ownerRole = roleRepository.findByRoleName("OWNER")
                    .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy ROLE_OWNER"));

            applicant.addRole(ownerRole);
            userRepository.save(applicant);
        }

        OwnerApplication savedApp = applicationRepository.save(application);

        if (applicant != null) {
            sendReviewNotificationEmail(applicant, savedApp);
        }

        return convertToDTO(savedApp);
    }

    private OwnerApplicationDTO convertToDTO(OwnerApplication app) {
        OwnerApplicationDTO dto = new OwnerApplicationDTO();
        dto.setId(app.getId());
        dto.setStatus(app.getStatus());
        dto.setPermanentAddress(app.getPermanentAddress());
        dto.setHometownAddress(app.getHometownAddress());
        dto.setCardFrontImage(app.getCardFrontImage());
        dto.setCardBackImage(app.getCardBackImage());
        dto.setBusinessLicenseImage(app.getBusinessLicenseImage());
        dto.setBusinessLicenseNumber(app.getBusinessLicenseNumber());
        dto.setCreatedAt(app.getCreatedAt());
        dto.setReviewedAt(app.getReviewedAt());
        dto.setAdminReason(app.getAdminReason());

        if (app.getUserId() != null) {
            User applicant = app.getUserId();
            dto.setApplicantId(applicant.getUserId());
            dto.setApplicantFullName(applicant.getFullName());
            dto.setApplicantEmail(applicant.getEmail());
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
        context.setVariable("reason", (adminReason != null && !adminReason.isEmpty()) ? adminReason : "N/A");

        String subject, templateName;
        if (status == ApplicationStatus.APPROVED) {
            subject = "Chúc mừng! Đơn đăng ký làm chủ khách sạn của bạn đã được DUYỆT";
            templateName = "email/application-approved";
        } else if (status == ApplicationStatus.REJECTED) {
            subject = "Thông báo: Đơn đăng ký làm chủ khách sạn của bạn đã bị TỪ CHỐI";
            templateName = "email/application-rejected";
        } else {
            return;
        }

        emailService.sendHtmlEmail(applicantEmail, subject, templateName, context);
    }
}