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
import org.springframework.beans.factory.annotation.Value; // ✅ Import Value
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile; // ✅ Import MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder; // ✅ Import ServletUriComponentsBuilder

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailService {
    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final FileStorageService fileStorageService; // ✅ Inject FileStorageService

    @Value("${file.static-url-prefix}") // ✅ Lấy prefix từ cấu hình (vd: /images)
    private String staticUrlPrefix;

    @Autowired
    public UserDetailService(UserRepository userRepository,
                             UserDetailRepository userDetailRepository,
                             FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Lấy thông tin hồ sơ kết hợp từ cả 2 bảng User và UserDetail.
     */
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Tự động tạo UserDetail nếu chưa có
        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElseGet(() -> {
                    UserDetail newUserDetail = new UserDetail();
                    newUserDetail.setUser(user);
                    return userDetailRepository.save(newUserDetail);
                });

        UserDetailResponseDTO responseDTO = new UserDetailResponseDTO();

        // Lấy từ User
        responseDTO.setUserId(user.getUserId()); // Thêm userId nếu DTO có trường này (tùy chọn)
        responseDTO.setEmail(user.getEmail());
        responseDTO.setFullName(user.getFullName());
        responseDTO.setPhoneNumber(user.getPhoneNumber());

        // Lấy từ UserDetail
        responseDTO.setUserdetailId(userDetail.getUserdetailId());
        responseDTO.setGender(userDetail.getGender());
        responseDTO.setDateOfBirth(userDetail.getDateOfBirth());
        responseDTO.setProfilePhotoUrl(userDetail.getProfilePhotoUrl());
        responseDTO.setAddress(userDetail.getAddress());
        responseDTO.setCity(userDetail.getCity());
        responseDTO.setCountry(userDetail.getCountry());

        return responseDTO;
    }

    /**
     * Cập nhật thông tin hồ sơ vào cả 2 bảng User và UserDetail.
     */
    @Transactional
    public UserDetailResponseDTO updateUserDetail(String email, UserDetailRequestDTO userDetailRequestDTO) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("UserDetail not found for user: " + email));

        // Cập nhật các trường của User
        user.setFullName(userDetailRequestDTO.getFullName());
        user.setPhoneNumber(userDetailRequestDTO.getPhoneNumber());
        userRepository.save(user);

        // Cập nhật các trường của UserDetail
        userDetail.setGender(userDetailRequestDTO.getGender());
        userDetail.setDateOfBirth(userDetailRequestDTO.getDateOfBirth());
        userDetail.setAddress(userDetailRequestDTO.getAddress());
        userDetail.setCity(userDetailRequestDTO.getCity());
        userDetail.setCountry(userDetailRequestDTO.getCountry());

        // Lưu ý: Không cập nhật profilePhotoUrl ở đây, vì có API riêng

        userDetailRepository.save(userDetail);

        return this.getUserDetail(email);
    }

    /**
     * ✅ HÀM MỚI: Upload ảnh đại diện
     */
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

        // =========================================================
        // 🗑️ BƯỚC 1: XÓA ẢNH CŨ (Nếu có)
        // =========================================================
        String oldUrl = userDetail.getProfilePhotoUrl();
        if (StringUtils.hasText(oldUrl)) {
            try {
                int index = oldUrl.indexOf(staticUrlPrefix);
                if (index != -1) {
                    String relativePath = oldUrl.substring(index + staticUrlPrefix.length() + 1);
                    fileStorageService.deleteFile(relativePath);
                }
            } catch (Exception e) {
                System.err.println("Không thể xóa ảnh cũ: " + e.getMessage());
            }
        }

        // =========================================================
        // 🆕 BƯỚC 2: LƯU ẢNH MỚI
        // =========================================================
        String fileName = fileStorageService.storeImageFile(file, "userdetail");

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(staticUrlPrefix + "/")
                .path(fileName)
                .toUriString();

        userDetail.setProfilePhotoUrl(fileDownloadUri);
        userDetailRepository.save(userDetail);

        return this.getUserDetail(user.getEmail());
    }

    /**
     * Kiểm tra xem các trường bắt buộc đã hoàn tất hay chưa.
     */
    @Transactional(readOnly = true)
    public ProfileStatusResponse checkProfileCompleteness(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElse(null);

        List<String> missingFields = new ArrayList<>();

        if (!StringUtils.hasText(user.getFullName())) missingFields.add("Họ và Tên");
        if (!StringUtils.hasText(user.getPhoneNumber())) missingFields.add("Số điện thoại");

        if (userDetail == null) {
            missingFields.add("Giới tính");
            missingFields.add("Ngày sinh");
        } else {
            if (!StringUtils.hasText(userDetail.getGender())) missingFields.add("Giới tính");
            if (userDetail.getDateOfBirth() == null) missingFields.add("Ngày sinh");
        }

        boolean isComplete = missingFields.isEmpty();
        return new ProfileStatusResponse(isComplete, isComplete ? null : missingFields);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileStatusResponse {
        private boolean isProfileComplete;
        private List<String> missingFields;
    }
}