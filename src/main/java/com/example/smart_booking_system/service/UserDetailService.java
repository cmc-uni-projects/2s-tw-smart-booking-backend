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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // ✅ Import Collectors

@Service
public class UserDetailService {
    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.static-url-prefix}")
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

        // ✅ [MỚI] Map Notification Email (Nếu null thì mặc định lấy email chính)
        responseDTO.setNotificationEmail(user.getNotificationEmail() != null ? user.getNotificationEmail() : user.getEmail());

        // ✅ [MỚI] Map Social Accounts (Danh sách các tài khoản liên kết)
        List<UserDetailResponseDTO.SocialAccountDTO> socialDTOs = user.getSocialAccounts().stream()
                .map(acc -> new UserDetailResponseDTO.SocialAccountDTO(acc.getProvider().toString(), acc.getEmail()))
                .collect(Collectors.toList());
        responseDTO.setSocialAccounts(socialDTOs);

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

        // ✅ [MỚI] Cập nhật Notification Email
        if (StringUtils.hasText(userDetailRequestDTO.getNotificationEmail())) {
            user.setNotificationEmail(userDetailRequestDTO.getNotificationEmail());
        }

        userRepository.save(user);

        // Cập nhật các trường của UserDetail
        userDetail.setGender(userDetailRequestDTO.getGender());
        userDetail.setDateOfBirth(userDetailRequestDTO.getDateOfBirth());
        userDetail.setAddress(userDetailRequestDTO.getAddress());
        userDetail.setCity(userDetailRequestDTO.getCity());
        userDetail.setCountry(userDetailRequestDTO.getCountry());

        userDetailRepository.save(userDetail);

        return this.getUserDetail(email);
    }

    /**
     * Upload ảnh đại diện
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

        // 🗑️ Xóa ảnh cũ
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

        // 🆕 Lưu ảnh mới
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
     * Kiểm tra hồ sơ hoàn tất
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