package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.PropertyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PropertyDetailRepository propertyDetailRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageService fileStorageService;

    @Override
    public PropertyDetailDTO submitPropertyApplication(PropertyApplicationSubmitDTO dto, List<MultipartFile> images, String ownerId) {
        // 1. Tìm Owner
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + ownerId));

        // 2. Map DTO -> Entity Property
        Property property = new Property();
        property.setOwner(owner);
        property.setPropertyName(dto.getPropertyName());
        property.setDescription(dto.getDescription());
        property.setPropertyType(dto.getPropertyType());

        // Địa chỉ
        property.setAddress(dto.getAddress());
        property.setCity(dto.getCity());
        property.setProvince(dto.getProvince());
        property.setCountry(dto.getCountry());

        // Contact info
        property.setPhoneContact(owner.getPhoneNumber());
        property.setEmailContact(owner.getEmail());

        // Giá trị mặc định
        property.setPostalCode("70000");
        property.setLatitude(BigDecimal.ZERO);
        property.setLongitude(BigDecimal.ZERO);
        property.setPropertyStatus(PropertyStatus.PENDING);
        property.setActive(false);
        property.setCreatedAt(LocalDate.now());
        property.setUpdatedAt(LocalDate.now());

        Property savedProperty = propertyRepository.save(property);

        // 3. Lưu Property Details (Diện tích)
        PropertyDetail detail = new PropertyDetail();
        detail.setProperty(savedProperty);
        detail.setArea(dto.getArea());
        propertyDetailRepository.save(detail);

        // 4. Xử lý Amenities (Map từ Frontend: {"wifi": true, "pool": false})
        if (dto.getAmenities() != null) {
            for (Map.Entry<String, Boolean> entry : dto.getAmenities().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    // Tìm Amenity trong DB (đã seed ở DataInitializer)
                    Amenity amenity = amenityRepository.findByAmenityNameAndAmenityType(entry.getKey(), AmenityType.PROPERTY)
                            .orElse(null);

                    if (amenity != null) {
                        PropertyAmenity pa = new PropertyAmenity();
                        pa.setProperty(savedProperty);
                        pa.setAmenity(amenity);
                        propertyAmenityRepository.save(pa);
                    }
                }
            }
        }

        // 5. Xử lý Hình ảnh
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                String imageUrl = fileStorageService.storeImageFile(file, "properties");

                PropertyImage pImage = new PropertyImage();
                pImage.setProperty(savedProperty);
                pImage.setImageUrl(imageUrl);
                pImage.setCover(i == 0); // Ảnh đầu tiên là ảnh bìa

                propertyImageRepository.save(pImage);
            }
        }

        // 6. Gửi email
        try {
            sendPropertySubmittedEmail(owner, savedProperty);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận: " + e.getMessage());
        }

        return mapToPropertyDetailDTO(savedProperty);
    }

    // ==================================================================
    // PHẦN 2: LOGIC CŨ & LOGIC MỚI (Lấy ảnh bìa)
    // ==================================================================

    @Override
    public Property addProperty(Property property, String ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner not found: " + ownerId));

        property.setOwner(owner);
        validateProperty(property);

        if (property.getPropertyStatus() == null) {
            property.setPropertyStatus(PropertyStatus.PENDING);
        }

        Property savedProperty = propertyRepository.save(property);
        try {
            sendPropertySubmittedEmail(owner, savedProperty);
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
        }
        return savedProperty;
    }

    @Override
    public List<Property> searchProperties(String city, String keyword) {
        if (city != null && city.trim().isEmpty()) city = null;
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        // Lưu ý: Hàm này trả về List<Property> gốc, nếu cần ảnh bìa bạn cần đổi sang DTO
        // hoặc frontend phải gọi thêm API chi tiết.
        return propertyRepository.searchProperties(city, keyword);
    }

    @Override
    public Property updateProperty(int id, Property updatedProperty) {
        Property existingProperty = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + id));

        if (updatedProperty.getPropertyName() != null) existingProperty.setPropertyName(updatedProperty.getPropertyName());
        if (updatedProperty.getAddress() != null) existingProperty.setAddress(updatedProperty.getAddress());
        if (updatedProperty.getCity() != null) existingProperty.setCity(updatedProperty.getCity());
        if (updatedProperty.getCountry() != null) existingProperty.setCountry(updatedProperty.getCountry());
        if (updatedProperty.getDescription() != null) existingProperty.setDescription(updatedProperty.getDescription());
        // ... (các trường khác tương tự)

        existingProperty.setUpdatedAt(LocalDate.now());
        return propertyRepository.save(existingProperty);
    }

    @Override
    public List<PropertyDetailDTO> getFeaturedProperties() {
        // ✅ CẬP NHẬT: Dùng hàm map để lấy kèm ảnh bìa
        return propertyRepository.findFeaturedProperties().stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status) {
        // ✅ CẬP NHẬT: Dùng hàm map để lấy kèm ảnh bìa cho trang Admin
        return propertyRepository.findByPropertyStatus(status).stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PropertyDetailDTO reviewProperty(Integer propertyId, PropertyReviewDTO reviewDTO, String adminUsername) {
        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getPropertyStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Property already reviewed");
        }

        PropertyStatus newStatus = PropertyStatus.valueOf(reviewDTO.getStatus().toUpperCase());
        property.setPropertyStatus(newStatus);
        property.setUpdatedAt(LocalDate.now());
        property.setActive(newStatus == PropertyStatus.APPROVE);

        Property savedProperty = propertyRepository.save(property);

        if (property.getOwner() != null) {
            sendPropertyReviewEmail(property.getOwner(), savedProperty, reviewDTO.getReason());
        }

        return mapToPropertyDetailDTO(savedProperty);
    }

    // --- Helper Methods ---

    // ✅ Helper MỚI: Map Entity -> DTO và lấy ảnh bìa
    private PropertyDetailDTO mapToPropertyDetailDTO(Property property) {
        // Giả sử PropertyDetailDTO có constructor nhận Property
        // Nếu chưa có, bạn có thể dùng setter như convertToFeaturedDTO cũ
        PropertyDetailDTO dto = new PropertyDetailDTO(property);

        // Logic lấy ảnh bìa
        Optional<PropertyImage> coverImage = propertyImageRepository.findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId());

        if (coverImage.isPresent()) {
            dto.setCoverImage(coverImage.get().getImageUrl()); // Đảm bảo DTO có setter này
        } else {
            // Fallback: Lấy ảnh bất kỳ nếu không có ảnh bìa
            propertyImageRepository.findFirstByProperty_PropertyId(property.getPropertyId())
                    .ifPresent(img -> dto.setCoverImage(img.getImageUrl()));
        }

        return dto;
    }

    private void validateProperty(Property property) {
        if (property.getAddress() == null || property.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Address cannot be empty");
    }

    private PropertyDetailDTO convertToFeaturedDTO(Property property) {
        // Hàm cũ này có thể thay thế bằng mapToPropertyDetailDTO
        return mapToPropertyDetailDTO(property);
    }

    private void sendPropertySubmittedEmail(User owner, Property property) {
        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());
        emailService.sendHtmlEmail(owner.getEmail(), "Xác nhận nộp đơn", "email/property-submitted-confirmation", context);
    }

    private void sendPropertyReviewEmail(User owner, Property property, String reason) {
        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());
        context.setVariable("reason", reason != null ? reason : "N/A");

        String template = property.getPropertyStatus() == PropertyStatus.APPROVE
                ? "email/property-approved" : "email/property-rejected";
        String subject = property.getPropertyStatus() == PropertyStatus.APPROVE
                ? "Cơ sở được duyệt" : "Cơ sở bị từ chối";

        emailService.sendHtmlEmail(owner.getEmail(), subject, template, context);
    }
}