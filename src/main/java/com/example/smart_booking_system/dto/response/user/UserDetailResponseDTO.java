package com.example.smart_booking_system.dto.response.user;

import com.example.smart_booking_system.entity.UserDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponseDTO {
    private Integer userdetailId;
    private String userId;

    // Thông tin từ bảng User
    private String fullName;
    private String email;
    private String phoneNumber;

    // Thông tin từ bảng UserDetail
    private String gender;
    private String profilePhotoUrl;
    private String address;
    private String city;
    private String country;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;

    public static UserDetailResponseDTO fromEntity(UserDetail userDetail) {
        if (userDetail == null) {
            return null;
        }
        // Lấy thông tin từ User entity liên kết
        String fullName = userDetail.getUser() != null ? userDetail.getUser().getFullName() : null;
        String email = userDetail.getUser() != null ? userDetail.getUser().getEmail() : null;
        String phone = userDetail.getUser() != null ? userDetail.getUser().getPhoneNumber() : null;

        return UserDetailResponseDTO.builder()
                .userdetailId(userDetail.getUserdetailId())
                .userId(userDetail.getUser().getUserId())
                .fullName(fullName)
                .email(email)
                .phoneNumber(phone)
                .gender(userDetail.getGender())
                .profilePhotoUrl(userDetail.getProfilePhotoUrl())
                .address(userDetail.getAddress())
                .city(userDetail.getCity())
                .country(userDetail.getCountry())
                .createdAt(userDetail.getCreatedAt())
                .updatedAt(userDetail.getUpdatedAt())
                .isActive(userDetail.isActive())
                .build();
    }
}