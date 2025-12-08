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
    // LẤY USER DETAIL
    // ==============================================================
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

        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setPoints(user.getPoints());
        dto.setMembershipRank(user.getMembershipRank());

        dto.setUserdetailId(userDetail.getUserdetailId());
        dto.setGender(userDetail.getGender());
        dto.setDateOfBirth(userDetail.getDateOfBirth());
        dto.setAddress(userDetail.getAddress());
        dto.setCity(userDetail.getCity());
        dto.setCountry(userDetail.getCountry());

        // Ảnh đại diện – TRẢ VỀ SIGNED URL nếu có
        if (StringUtils.hasText(userDetail.getProfilePhotoUrl())) {
            dto.setProfilePhotoUrl(
                    fileStorageService.generateSignedUrl(userDetail.getProfilePhotoUrl())
            );
        }

        return dto;
    }

    // ==============================================================
    // CẬP NHẬT THÔNG TIN USER
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO updateUserDetail(String email, UserDetailRequestDTO req) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserDetail not found for user: " + email));

        user.setFullName(req.getFullName());
        user.setPhoneNumber(req.getPhoneNumber());
        userRepository.save(user);

        detail.setGender(req.getGender());
        detail.setDateOfBirth(req.getDateOfBirth());
        detail.setAddress(req.getAddress());
        detail.setCity(req.getCity());
        detail.setCountry(req.getCountry());

        userDetailRepository.save(detail);

        return getUserDetail(email);
    }

    // ==============================================================
    // UPLOAD ẢNH ĐẠI DIỆN (R2 PRIVATE + SIGNED URL)
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

        // 🗑️ XÓA ẢNH CŨ TRÊN R2 (nếu có)
        if (StringUtils.hasText(detail.getProfilePhotoUrl())) {
            fileStorageService.deleteFile(detail.getProfilePhotoUrl()); // key = đường dẫn trong R2
        }

        // 📤 UPLOAD ẢNH MỚI
        String key = fileStorageService.storeImageFile(file, "userdetail");

        // Lưu KEY vào DB, KHÔNG lưu full URL
        detail.setProfilePhotoUrl(key);
        userDetailRepository.save(detail);

        return getUserDetail(user.getEmail());
    }

    // ==============================================================
    // KIỂM TRA HỒ SƠ ĐẦY ĐỦ CHƯA
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
