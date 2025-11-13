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
    private String staticUrlPrefix;

    // 1. LẤY DANH SÁCH (LIST)
    @Transactional(readOnly = true)
    public List<UserDetailResponseDTO> getAllActiveUserDetails() {
        List<UserDetail> detailsList = userDetailRepository.findAllByIsActiveTrue();
        return detailsList.stream()
                .map(UserDetailResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // 2. THÊM MỚI (ADD)
    public UserDetailResponseDTO createUserDetail(UserDetailRequestDTO dto, String userId) {
        Optional<UserDetail> existingDetailOpt = userDetailRepository.findByUserUserId(userId);

        if (existingDetailOpt.isPresent()) {
            UserDetail existingDetail = existingDetailOpt.get();
            if (existingDetail.isActive()) {
                throw new ConflictException("Thông tin cá nhân đã tồn tại. Vui lòng dùng chức năng cập nhật.");
            } else {
                return reactivateAndUpdateUserDetail(existingDetail, dto);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        // --- UPDATE THÔNG TIN USER GỐC ---
        if (dto.getFullName() != null && !dto.getFullName().isEmpty()) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isEmpty()) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        // JPA sẽ tự động update User khi transaction commit, nhưng save rõ ràng cũng tốt
        userRepository.save(user);

        UserDetail newUserDetail = new UserDetail();
        newUserDetail.setUser(user);
        mapDtoToEntity(dto, newUserDetail); // Hàm helper map dữ liệu
        newUserDetail.setActive(true);

        UserDetail savedDetail = userDetailRepository.save(newUserDetail);
        return UserDetailResponseDTO.fromEntity(savedDetail);
    }

    // 3. TÌM KIẾM (SEARCH BY ID)
    @Transactional(readOnly = true)
    public UserDetailResponseDTO getUserDetailByUserId(String userId) {
        // Nếu chưa có UserDetail, ta vẫn nên trả về thông tin cơ bản (Tên, Email) thay vì lỗi 404
        // để Frontend form có dữ liệu hiển thị
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        UserDetail userDetail = userDetailRepository.findActiveByUserId(userId).orElse(null);

        if (userDetail == null) {
            // Tạo object ảo để trả về thông tin cơ bản cho form
            userDetail = new UserDetail();
            userDetail.setUser(user);
            // Các trường khác null
        }

        return UserDetailResponseDTO.fromEntity(userDetail);
    }

    // 4. CẬP NHẬT (EDIT)
    public UserDetailResponseDTO updateUserDetail(UserDetailRequestDTO dto, String userId) {
        UserDetail existingDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có thông tin chi tiết. Vui lòng tạo mới trước."));

        User user = existingDetail.getUser();

        // --- UPDATE THÔNG TIN USER GỐC (Phone, FullName) ---
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        userRepository.save(user);

        // --- UPDATE THÔNG TIN CHI TIẾT ---
        mapDtoToEntity(dto, existingDetail);

        UserDetail updatedDetail = userDetailRepository.save(existingDetail);
        return UserDetailResponseDTO.fromEntity(updatedDetail);
    }

    // 5. XÓA MỀM
    public String deleteUserDetail(String userId) {
        UserDetail existingDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin để xóa."));
        existingDetail.setActive(false);
        userDetailRepository.save(existingDetail);
        return "Đã xóa thông tin cá nhân thành công.";
    }

    // UPLOAD ẢNH
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {
        UserDetail userDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ để tải ảnh."));

        String savedFileName = fileStorageService.storeImageFile(file, "userdetail");
        String imageUrl = staticUrlPrefix + "/" + savedFileName;

        userDetail.setProfilePhotoUrl(imageUrl);
        UserDetail updatedDetail = userDetailRepository.save(userDetail);
        return UserDetailResponseDTO.fromEntity(updatedDetail);
    }

    // HELPER: Reactivate
    private UserDetailResponseDTO reactivateAndUpdateUserDetail(UserDetail existingDetail, UserDetailRequestDTO dto) {
        existingDetail.setActive(true);

        // Update cả User gốc khi reactivate
        User user = existingDetail.getUser();
        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        userRepository.save(user);

        mapDtoToEntity(dto, existingDetail);

        UserDetail updatedDetail = userDetailRepository.save(existingDetail);
        return UserDetailResponseDTO.fromEntity(updatedDetail);
    }

    // HELPER: Map DTO to Entity (Tránh lặp code)
    private void mapDtoToEntity(UserDetailRequestDTO dto, UserDetail entity) {
        if (dto.getGender() != null) entity.setGender(dto.getGender());
        if (dto.getProfilePhotoUrl() != null) entity.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getCountry() != null) entity.setCountry(dto.getCountry());
    }
}