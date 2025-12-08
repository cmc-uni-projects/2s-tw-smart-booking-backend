package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public UserDetailService(UserRepository userRepository,
                             UserDetailRepository userDetailRepository,
                             FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lấy thông tin hồ sơ từ User + UserDetail
     */
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        UserDetailResponseDTO dto = new UserDetailResponseDTO();

        // User info
        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPoints(user.getPoints());
        dto.setMembershipRank(user.getMembershipRank());

        // Detail info
        dto.setUserdetailId(userDetail.getUserdetailId());
        dto.setGender(userDetail.getGender());
        dto.setDateOfBirth(userDetail.getDateOfBirth());
        dto.setProfilePhotoUrl(userDetail.getProfilePhotoUrl());
        dto.setAddress(userDetail.getAddress());
        dto.setCity(userDetail.getCity());
        dto.setCountry(userDetail.getCountry());

        return dto;
    }

    /**
     * Cập nhật thông tin User + UserDetail
     */
    @Transactional
    public UserDetailResponseDTO updateUserDetail(String email, UserDetailRequestDTO req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserDetail not found"));

        // Update User
        user.setFullName(req.getFullName());
        user.setPhoneNumber(req.getPhoneNumber());
        userRepository.save(user);

        // Update Detail
        detail.setGender(req.getGender());
        detail.setDateOfBirth(req.getDateOfBirth());
        detail.setAddress(req.getAddress());
        detail.setCity(req.getCity());
        detail.setCountry(req.getCountry());
        userDetailRepository.save(detail);

        return this.getUserDetail(email);
    }

    /**
     * Upload avatar (Cloudinary)
     */
    @Transactional
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        // ======================
        // XÓA ẢNH CŨ TRÊN CLOUDINARY
        // ======================
        String oldUrl = detail.getProfilePhotoUrl();
        if (StringUtils.hasText(oldUrl)) {
            try {
                fileStorageService.deleteFile(oldUrl);
            } catch (Exception e) {
                System.err.println("Không thể xóa avatar cũ: " + e.getMessage());
            }
        }

        // ======================
        // UPLOAD ẢNH MỚI
        // ======================
        String cloudUrl = fileStorageService.storeImageFile(file, "user_avatars");

        // Lưu URL CLOUDINARY vào DB
        detail.setProfilePhotoUrl(cloudUrl);
        userDetailRepository.save(detail);

        return this.getUserDetail(user.getEmail());
    }

    /**
     * Check profile completeness
     */
    @Transactional(readOnly = true)
    public ProfileStatusResponse checkProfileCompleteness(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        boolean complete = missing.isEmpty();
        return new ProfileStatusResponse(complete, complete ? null : missing);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileStatusResponse {
        private boolean isProfileComplete;
        private List<String> missingFields;
    }
}
