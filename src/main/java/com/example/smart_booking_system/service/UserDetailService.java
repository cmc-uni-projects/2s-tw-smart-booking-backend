package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final FileStorageService fileStorageService;

    public UserDetailService(UserRepository userRepository,
                             UserDetailRepository userDetailRepository,
                             FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
        this.fileStorageService = fileStorageService;
    }

    // ==============================================================
    // GET USER DETAIL — TRẢ VỀ SIGNED URL
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        UserDetailResponseDTO dto = new UserDetailResponseDTO();

        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPoints(user.getPoints());
        dto.setMembershipRank(user.getMembershipRank());

        dto.setUserdetailId(detail.getUserdetailId());
        dto.setGender(detail.getGender());
        dto.setDateOfBirth(detail.getDateOfBirth());
        dto.setAddress(detail.getAddress());
        dto.setCity(detail.getCity());
        dto.setCountry(detail.getCountry());

        // Avatar: trả về signedURL
        if (StringUtils.hasText(detail.getProfilePhotoUrl())) {
            dto.setProfilePhotoUrl(
                    fileStorageService.generateSignedUrl(detail.getProfilePhotoUrl())
            );
        }

        return dto;
    }

    // ==============================================================
    // UPDATE USER DETAIL
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO updateUserDetail(String email, UserDetailRequestDTO req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserDetail not found for user: " + email));

        user.setFullName(req.getFullName());
        user.setPhoneNumber(req.getPhoneNumber());
        if (StringUtils.hasText(req.getNotificationEmail())) {
            user.setNotificationEmail(req.getNotificationEmail());
        }
        userRepository.save(user);

        detail.setGender(req.getGender());
        detail.setDateOfBirth(req.getDateOfBirth());
        detail.setAddress(req.getAddress());
        detail.setCity(req.getCity());
        detail.setCountry(req.getCountry());

        userDetailRepository.save(detail);

        return this.getUserDetail(email);
    }

    // ==============================================================
    // UPLOAD PROFILE PHOTO — LUÔN LƯU VÀO R2
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        // Xóa ảnh cũ trên R2
        if (StringUtils.hasText(detail.getProfilePhotoUrl())) {
            fileStorageService.deleteFile(detail.getProfilePhotoUrl());
        }

        // Upload ảnh mới
        String key = fileStorageService.storeImageFile(file, "userdetail");

        // Lưu KEY, không lưu URL
        detail.setProfilePhotoUrl(key);
        userDetailRepository.save(detail);

        return getUserDetail(user.getEmail());
    }

    // ==============================================================
    // CHECK PROFILE COMPLETENESS
    // ==============================================================
    @Transactional(readOnly = true)
    public ProfileStatusResponse checkProfileCompleteness(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        UserDetail detail = userDetailRepository.findByUser(user).orElse(null);

        List<String> missing = new ArrayList<>();

        if (!StringUtils.hasText(user.getFullName())) missing.add("Họ và Tên");
        if (!StringUtils.hasText(user.getPhoneNumber())) missing.add("Số điện thoại");

        if (detail == null) {
            missing.add("Giới tính");
            missing.add("Ngày sinh");
        } else {
            if (!StringUtils.hasText(detail.getGender())) missing.add("Giới tính");
            if (detail.getDateOfBirth() == null) missing.add("Ngày sinh");
        }

        return new ProfileStatusResponse(missing.isEmpty(), missing.isEmpty() ? null : missing);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileStatusResponse {
        private boolean isProfileComplete;
        private List<String> missingFields;
    }
}
