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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <-- Import quan trọng
import org.springframework.util.StringUtils; // <-- Import quan trọng

@Service
public class UserDetailService {
    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;

    @Autowired
    public UserDetailService(UserRepository userRepository, UserDetailRepository userDetailRepository) {
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
    }

    /**
     * Lấy thông tin hồ sơ kết hợp từ cả 2 bảng User và UserDetail.
     */
    // === SỬA LỖI 500 ===
    // Đã xóa (readOnly = true) để cho phép .save() trong orElseGet()
    @Transactional
    public UserDetailResponseDTO getUserDetail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Tự động tạo UserDetail nếu chưa có
        // Logic này cần Transaction (không thể readOnly)
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

        // Lấy từ UserDetail (đã thêm dateOfBirth)
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
     * Trả về DTO đã được cập nhật.
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
        userRepository.save(user); // Lưu thay đổi của User

        // Cập nhật các trường của UserDetail (đã thêm dateOfBirth)
        userDetail.setGender(userDetailRequestDTO.getGender());
        userDetail.setDateOfBirth(userDetailRequestDTO.getDateOfBirth());
        userDetail.setProfilePhotoUrl(userDetailRequestDTO.getProfilePhotoUrl());
        userDetail.setAddress(userDetailRequestDTO.getAddress());
        userDetail.setCity(userDetailRequestDTO.getCity());
        userDetail.setCountry(userDetailRequestDTO.getCountry());
        userDetailRepository.save(userDetail); // Lưu thay đổi của UserDetail

        // Trả về DTO đã cập nhật (để sửa Lỗi 3 của ProfilePage)
        return this.getUserDetail(email);
    }

    /**
     * (Giai đoạn 2) Kiểm tra xem 4 trường bắt buộc đã hoàn tất hay chưa.
     */
    @Transactional(readOnly = true)
    public ProfileStatusResponse checkProfileCompleteness(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        UserDetail userDetail = userDetailRepository.findByUser(user)
                .orElse(null); // Có thể user chưa có UserDetail

        // Kiểm tra trường của User
        boolean isUserComplete = StringUtils.hasText(user.getFullName()) &&
                StringUtils.hasText(user.getPhoneNumber());

        // Kiểm tra trường của UserDetail
        boolean isDetailComplete = false;
        if (userDetail != null) {
            isDetailComplete = StringUtils.hasText(userDetail.getGender()) &&
                    userDetail.getDateOfBirth() != null;
        }

        return new ProfileStatusResponse(isUserComplete && isDetailComplete);
    }

    /**
     * DTO nội bộ (Inner class) để trả về trạng thái hồ sơ
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileStatusResponse {
        private boolean isProfileComplete;
    }
}