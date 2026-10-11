package com.example.smart_booking_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.i18n.I18nConfig;
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

    @Autowired
    private Messages messages;

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

    /** Saves the language used for this user's emails and notifications ("vi" or "en"). */
    @Transactional
    public String updatePreferredLanguage(String userId, String language) {
        if (language == null || !I18nConfig.SUPPORTED.contains(java.util.Locale.forLanguageTag(language))) {
            throw new BadRequestException("error.language.unsupported", language);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound"));
        user.setPreferredLanguage(language);
        return language;
    }

    // ==============================================================
    // GET USER DETAIL — TRẢ VỀ SIGNED URL
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", email));

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
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", email));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", email));

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

        // 1. Validate file
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.file.invalidImage");
        }

        // 2. Lấy User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", userId));

        // 3. Lấy hoặc tạo UserDetail
        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        // 4. Xóa ảnh cũ (CHỈ KHI LÀ KEY, KHÔNG PHẢI URL)
        String oldValue = detail.getProfilePhotoUrl();
        if (StringUtils.hasText(oldValue) && !oldValue.startsWith("http")) {
            fileStorageService.deleteFile(oldValue);
        }

        // 5. Upload ảnh mới lên R2
        String key = fileStorageService.storeImageFile(file, "userdetail");

        // 6. Lưu KEY vào DB
        detail.setProfilePhotoUrl(key);
        userDetailRepository.save(detail);

        // 7. Trả về DTO (getUserDetail phải convert KEY -> URL)
        return getUserDetail(user.getEmail());
    }


    // ==============================================================
    // CHECK PROFILE COMPLETENESS
    // ==============================================================
    @Transactional(readOnly = true)
    public ProfileStatusResponse checkProfileCompleteness(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("error.user.notFound", email));

        UserDetail detail = userDetailRepository.findByUser(user).orElse(null);

        List<String> missing = new ArrayList<>();

        if (!StringUtils.hasText(user.getFullName())) missing.add(messages.t("profile.field.fullName"));
        if (!StringUtils.hasText(user.getPhoneNumber())) missing.add(messages.t("profile.field.phone"));

        if (detail == null) {
            missing.add(messages.t("profile.field.gender"));
            missing.add(messages.t("profile.field.dob"));
        } else {
            if (!StringUtils.hasText(detail.getGender())) missing.add(messages.t("profile.field.gender"));
            if (detail.getDateOfBirth() == null) missing.add(messages.t("profile.field.dob"));
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
