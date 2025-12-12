package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.response.admin.AdminUserResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManageUserService {

    private final UserRepository userRepository;

    // 1. Lấy danh sách user
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getAllUsers(String keyword, String role, String status, Pageable pageable) {
        Page<User> userPage = userRepository.findUsersWithFilter(keyword, role, status, pageable);

        return userPage.map(this::convertToDTO);
    }

    // 2. Lấy chi tiết user
    @Transactional(readOnly = true)
    public AdminUserResponseDTO getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return convertToDTO(user);
    }

    // 3. Cập nhật trạng thái (Khóa/Mở khóa)
    @Transactional
    public void updateUserStatus(String userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(status.toUpperCase());
        userRepository.save(user);
    }

    // --- Helper Methods ---

    private AdminUserResponseDTO convertToDTO(User user) {
        return new AdminUserResponseDTO(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getIsEmailVerified(),
                user.getPoints(),
                user.getMembershipRank(),
                user.getCreatedAt(),
                user.getRoles().stream().map(r -> r.getRoleName()).collect(Collectors.toSet())
        );
    }
}