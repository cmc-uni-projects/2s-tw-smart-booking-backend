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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserDetailService {

    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.static-url-prefix}")
    private String staticUrlPrefix;

    public UserDetailService(UserRepository userRepository,
                             UserDetailRepository userDetailRepository,
                             FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
        this.fileStorageService = fileStorageService;
    }

    // ==============================================================
    // GET USER DETAIL (FILE 1)
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newUserDetail = new UserDetail();
                    newUserDetail.setUser(user);
                    return userDetailRepository.save(newUserDetail);
                });

        UserDetailResponseDTO responseDTO = new UserDetailResponseDTO();

        // Lấy từ User
        responseDTO.setUserId(user.getUserId());
        responseDTO.setEmail(user.getEmail());
        responseDTO.setFullName(user.getFullName());
        responseDTO.setPhoneNumber(user.getPhoneNumber());
        responseDTO.setPoints(user.getPoints());
        responseDTO.setMembershipRank(user.getMembershipRank());

        // Lấy từ UserDetail
        responseDTO.setUserdetailId(userDetail.getUserdetailId());
        responseDTO.setGender(userDetail.getGender());
        responseDTO.setDateOfBirth(userDetail.getDateOfBirth());
        responseDTO.setProfilePhotoUrl(userDetail.getProfilePhotoUrl());
        responseDTO.setAddress(userDetail.getAddress());
        responseDTO.setCity(userDetail.getCity());
        responseDTO.setCountry(userDetail.getCountry());

        // Notification Email
        responseDTO.setNotificationEmail(
                user.getNotificationEmail() != null ? user.getNotificationEmail() : user.getEmail()
        );

        // Social Accounts
        List<UserDetailResponseDTO.SocialAccountDTO> socialDTOs = user.getSocialAccounts().stream()
                .map(acc -> new UserDetailResponseDTO.SocialAccountDTO(acc.getProvider().toString(), acc.getEmail()))
                .collect(Collectors.toList());
        responseDTO.setSocialAccounts(socialDTOs);

        return responseDTO;
    }

    // ==============================================================
    // GET USER DETAIL (FILE 2 - PHẦN KHÁC BIỆT GIỮ LẠI)
    //  → Logic khác: trả về signedURL nếu có
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO getUserDetailSigned(String email) {

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

        if (StringUtils.hasText(userDetail.getProfilePhotoUrl())) {
            dto.setProfilePhotoUrl(
                    fileStorageService.generateSignedUrl(userDetail.getProfilePhotoUrl())
            );
        }

        return dto;
    }

    // ==============================================================
    // UPDATE USER DETAIL (FILE 1)
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
    // UPLOAD PROFILE PHOTO (FILE 1)
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newUserDetail = new UserDetail();
                    newUserDetail.setUser(user);
                    return userDetailRepository.save(newUserDetail);
                });

        String oldUrl = userDetail.getProfilePhotoUrl();
        if (StringUtils.hasText(oldUrl)) {
            try {
                int index = oldUrl.indexOf(staticUrlPrefix);
                if (index != -1) {
                    String relativePath = oldUrl.substring(index + staticUrlPrefix.length() + 1);
                    fileStorageService.deleteFile(relativePath);
                }
            } catch (Exception ignored) {}
        }

        String fileName = fileStorageService.storeImageFile(file, "userdetail");

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(staticUrlPrefix + "/")
                .path(fileName)
                .toUriString();

        userDetail.setProfilePhotoUrl(fileDownloadUri);
        userDetailRepository.save(userDetail);

        return this.getUserDetail(user.getEmail());
    }

    // ==============================================================
    // UPLOAD PROFILE PHOTO (FILE 2 - KHÁC BIỆT GIỮ LẠI)
    // ==============================================================
    @Transactional
    public UserDetailResponseDTO uploadProfilePhotoR2(String userId, MultipartFile file) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UserDetail detail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newDetail = new UserDetail();
                    newDetail.setUser(user);
                    return userDetailRepository.save(newDetail);
                });

        if (StringUtils.hasText(detail.getProfilePhotoUrl())) {
            fileStorageService.deleteFile(detail.getProfilePhotoUrl());
        }

        String key = fileStorageService.storeImageFile(file, "userdetail");

        detail.setProfilePhotoUrl(key);
        userDetailRepository.save(detail);

        return getUserDetailSigned(user.getEmail());
    }

    // ==============================================================
    // CHECK PROFILE (FILE 1 + FILE 2 GIỐNG NHAU → GIỮ 1 BẢN)
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
