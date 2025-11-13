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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserDetailService {

    private final UserDetailRepository userDetailRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.static-url-prefix}")
    private String staticUrlPrefix; // Ví dụ: /images

    // Lấy thông tin chi tiết (chỉ lấy active)
    @Transactional(readOnly = true)
    public UserDetailResponseDTO getUserDetailByUserId(String userId) {
        UserDetail userDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết người dùng (hoặc đã bị vô hiệu hóa) cho ID: " + userId));
        return UserDetailResponseDTO.fromEntity(userDetail);
    }

    // Tạo thông tin chi tiết
    public UserDetailResponseDTO createUserDetail(UserDetailRequestDTO dto, String userId) {

        Optional<UserDetail> existingDetailOpt = userDetailRepository.findByUserUserId(userId);

        if (existingDetailOpt.isPresent()) {
            UserDetail existingDetail = existingDetailOpt.get();
            if (existingDetail.isActive()) {
                // Nếu đang active -> báo lỗi
                throw new ConflictException("Chi tiết người dùng đã tồn tại. Vui lòng sử dụng PUT để cập nhật.");
            } else {
                // Nếu đang inactive -> kích hoạt lại và cập nhật
                return reactivateAndUpdateUserDetail(existingDetail, dto);
            }
        }

        // Nếu chưa tồn tại -> tạo mới
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

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


    // Cập nhật thông tin chi tiết (chỉ cập nhật active)
    public UserDetailResponseDTO updateUserDetail(UserDetailRequestDTO dto, String userId) {

        UserDetail existingDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết người dùng (hoặc đã bị vô hiệu hóa) cho ID: " + userId));

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

    // Xóa thông tin chi tiết
    public String deleteUserDetail(String userId) {

        UserDetail existingDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết người dùng (hoặc đã bị vô hiệu hóa) cho ID: " + userId));


        existingDetail.setActive(false);
        userDetailRepository.save(existingDetail);

        return "Chi tiết người dùng cho ID " + userId + " đã được vô hiệu hóa thành công.";
    }

    /**
     * Xử lý upload ảnh đại diện cho người dùng
     * @param userId ID của người dùng
     * @param file File ảnh
     * @return DTO đã cập nhật
     */
    public UserDetailResponseDTO uploadProfilePhoto(String userId, MultipartFile file) {
        // 1. Tìm UserDetail (chỉ tìm active)
        UserDetail userDetail = userDetailRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi tiết người dùng cho ID: " + userId));

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