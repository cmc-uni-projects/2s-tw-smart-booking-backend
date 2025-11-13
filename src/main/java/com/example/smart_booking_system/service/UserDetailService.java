package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.user.UserDetailRequestDTO;
import com.example.smart_booking_system.dto.response.user.UserDetailResponseDTO;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.entity.UserDetail;
import com.example.smart_booking_system.exception.ConflictException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.UserDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserDetailService {

    private final UserDetailRepository userDetailRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.static-url-prefix}")
    private String staticUrlPrefix; // Ví dụ: /images

    // ==========================================================
    // 1. LẤY DANH SÁCH (LIST)
    // ==========================================================
    @Transactional(readOnly = true)
    public List<UserDetailResponseDTO> getAllActiveUserDetails() {
        List<UserDetail> detailsList = userDetailRepository.findAllByIsActiveTrue();
        return detailsList.stream()
                .map(UserDetailResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // 2. THÊM MỚI (ADD)
    // ==========================================================
    public UserDetailResponseDTO createUserDetail(UserDetailRequestDTO dto, String userId) {

        Optional<UserDetail> existingDetailOpt = userDetailRepository.findByUserUserId(userId);

        if (existingDetailOpt.isPresent()) {
            UserDetail existingDetail = existingDetailOpt.get();
            if (existingDetail.isActive()) {
                // === SỬA ĐỔI VĂN BẢN ===
                throw new ConflictException("Thông tin cá nhân của bạn đã tồn tại. Bạn có thể sử dụng chức năng 'Cập nhật' để thay đổi.");
            } else {
                // Nếu đang inactive -> kích hoạt lại và cập nhật
                return reactivateAndUpdateUserDetail(existingDetail, dto);
            }
        }

        // Nếu chưa tồn tại -> tạo mới
        User user = userRepository.findById(userId)
                // === SỬA ĐỔI VĂN BẢN ===
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng để liên kết thông tin cá nhân."));

        UserDetail newUserDetail = new UserDetail();
        newUserDetail.setUser(user);
        newUserDetail.setGender(dto.getGender());
        newUserDetail.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        newUserDetail.setAddress(dto.getAddress());
        newUserDetail.setCity(dto.getCity());
        newUserDetail.setCountry(dto.getCountry());
        newUserDetail.setActive(true); // Đảm bảo active

        UserDetail savedDetail = userDetailRepository.save(newUserDetail);
        return UserDetailResponseDTO.fromEntity(savedDetail);
    }

    // ==========================================================
    // 3. TÌM KIẾM (SEARCH BY ID)
    // ==========================================================
    @Transactional(readOnly = true)
    public UserDetailResponseDTO getUserDetailByUserId(String userId) {
        UserDetail userDetail = userDetailRepository.findActiveByUserId(userId)
                // === SỬA ĐỔI VĂN BẢN ===
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin cá nhân cho tài khoản này."));
        return UserDetailResponseDTO.fromEntity(userDetail);
    }

    // ==========================================================
    // 4. CẬP NHẬT (EDIT)
    // ==========================================================
    public UserDetailResponseDTO updateUserDetail(UserDetailRequestDTO dto, String userId) {

        UserDetail existingDetail = userDetailRepository.findActiveByUserId(userId)
                // === SỬA ĐỔI VĂN BẢN ===
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin cá nhân để cập nhật."));

        // Cập nhật từng phần
        if (dto.getGender() != null) {
            existingDetail.setGender(dto.getGender());
        }
        if (dto.getProfilePhotoUrl() != null) {
            existingDetail.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        }
        if (dto.getAddress() != null) {
            existingDetail.setAddress(dto.getAddress());
        }
        if (dto.getCity() != null) {
            existingDetail.setCity(dto.getCity());
        }
        if (dto.getCountry() != null) {
            existingDetail.setCountry(dto.getCountry());
        }

        UserDetail updatedDetail = userDetailRepository.save(existingDetail);
        return UserDetailResponseDTO.fromEntity(updatedDetail);
    }

    // ==========================================================
    // 5. XÓA MỀM (DELETE)
    // ==========================================================
    public String deleteUserDetail(String userId) {

        UserDetail existingDetail = userDetailRepository.findActiveByUserId(userId)
                // === SỬA ĐỔI VĂN BẢN ===
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin cá nhân để xóa."));


        existingDetail.setActive(false);
        userDetailRepository.save(existingDetail);

        // === SỬA ĐỔI VĂN BẢN ===
        return "Đã xóa thông tin cá nhân thành công.";
    }

    // ==========================================================
    // CÁC HÀM PHỤ (UPLOAD, HELPERS)
    // ==========================================================

    /**
     * Xử lý upload ảnh đại diện cho người dùng
     * @param userId ID của người dùng
     * @param file File ảnh
     * @return DTO đã cập nhật
     */
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {
        // 1. Tìm UserDetail (chỉ tìm active)
        UserDetail userDetail = userDetailRepository.findActiveByUserId(userId)
                // === SỬA ĐỔI VĂN BẢN ===
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ cá nhân để tải ảnh lên."));

        // 2. Gọi FileStorageService và chỉ định thư mục con là "userdetail"
        String savedFileName = fileStorageService.storeImageFile(file, "userdetail");

        // 3. Tạo đường dẫn URL để truy cập ảnh
        // (Ví dụ: /images/abc-123.png)
        String imageUrl = staticUrlPrefix + "/" + savedFileName;

        // 4. Cập nhật URL vào database
        userDetail.setProfilePhotoUrl(imageUrl);
        UserDetail updatedDetail = userDetailRepository.save(userDetail);

        // 5. Trả về DTO
        return UserDetailResponseDTO.fromEntity(updatedDetail);
    }

    // Helper: Kích hoạt lại và cập nhật
    private UserDetailResponseDTO reactivateAndUpdateUserDetail(UserDetail existingDetail, UserDetailRequestDTO dto) {
        existingDetail.setActive(true);
        existingDetail.setGender(dto.getGender());
        existingDetail.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        existingDetail.setAddress(dto.getAddress());
        existingDetail.setCity(dto.getCity());
        existingDetail.setCountry(dto.getCountry());

        UserDetail updatedDetail = userDetailRepository.save(existingDetail);
        return UserDetailResponseDTO.fromEntity(updatedDetail);
    }

    // Helper: Xóa file ảnh cũ
    private void deleteOldImageFile(String oldImageUrl) {
        // Kiểm tra xem có ảnh cũ không, và nó có phải là ảnh do hệ thống quản lý không
        if (oldImageUrl == null || oldImageUrl.trim().isEmpty() || !oldImageUrl.startsWith(staticUrlPrefix + "/")) {
            // Nếu rỗng, hoặc là ảnh mặc định (ví dụ: /default-avatar.png), thì không xóa
            return;
        }

        try {
            // Tách lấy đường dẫn tương đối (ví dụ: "/images/avatars/abc.png" -> "avatars/abc.png")
            String oldRelativePath = oldImageUrl.substring(staticUrlPrefix.length() + 1);

            // Gọi service để xóa file vật lý
            fileStorageService.deleteFile(oldRelativePath);

        } catch (Exception e) {
            // Ghi log nếu có lỗi, nhưng không ném exception
            System.err.println("Lỗi khi tách/xóa đường dẫn file cũ: " + oldImageUrl + " - " + e.getMessage());
        }
    }
}