package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.application.OwnerApplicationSubmitDTO;
import com.example.smart_booking_system.dto.request.admin.OwnerApplicationReviewDTO;
import com.example.smart_booking_system.dto.response.admin.OwnerApplicationDTO;
import com.example.smart_booking_system.enums.ApplicationStatus;

import java.util.List;

public interface OwnerApplicationService {

    OwnerApplicationDTO submitApplication(OwnerApplicationSubmitDTO submitDTO, String applicantUsername);

    List<OwnerApplicationDTO> getApplicationsByStatus(ApplicationStatus status);

    OwnerApplicationDTO reviewApplication(Long applicationId, OwnerApplicationReviewDTO reviewDTO, String adminUsername);
}