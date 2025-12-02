package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.admin.AdminResetPasswordDTO;
import com.example.smart_booking_system.dto.request.admin.UpdateMembershipDTO;
import com.example.smart_booking_system.dto.response.admin.AdminUserResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.MembershipRank;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManageUserService { //

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 1. Lấy danh sách user (Phân trang nằm ở biến Pageable pageable)
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getAllUsers(String keyword, String role, String status, Pageable pageable) {
        // Spring Data JPA sẽ tự động dùng 'pageable' để thêm LIMIT và OFFSET vào câu Query
        Page<User> userPage = userRepository.findUsersWithFilter(keyword, role, status, pageable);

        // Hàm map() này cũng hỗ trợ giữ nguyên thông tin phân trang (totalPage, totalElements)
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

    // 4. Cập nhật Membership & Điểm
    @Transactional
    public void updateUserMembership(String userId, UpdateMembershipDTO updateDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (updateDTO.getPoints() != null) {
            if (updateDTO.isAddPoints()) {
                user.setPoints(user.getPoints() + updateDTO.getPoints());
            } else {
                user.setPoints(updateDTO.getPoints());
            }
        }

        if (updateDTO.getRank() != null) {
            user.setMembershipRank(updateDTO.getRank());
        } else {
            updateRankBasedOnPoints(user);
        }
        userRepository.save(user);
    }

    // 5. Reset mật khẩu
    @Transactional
    public void resetUserPassword(String userId, AdminResetPasswordDTO resetDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(resetDTO.getNewPassword()));
        userRepository.save(user);
    }

    // --- Helper Methods ---

    private void updateRankBasedOnPoints(User user) {
        int p = user.getPoints();
        if (p >= 5000) user.setMembershipRank(MembershipRank.DIAMOND);
        else if (p >= 1000) user.setMembershipRank(MembershipRank.GOLD);
        else if (p >= 500) user.setMembershipRank(MembershipRank.SILVER);
        else user.setMembershipRank(MembershipRank.BRONZE);
    }

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