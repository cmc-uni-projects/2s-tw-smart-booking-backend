package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import com.example.smart_booking_system.dto.RoomResponseDTO;
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
import com.example.smart_booking_system.repository.RoomImageRepository; // ✅ Import Repository
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final RoomImageRepository roomImageRepository;

    // ==================================================================
    // 1. LOGIC NỘP ĐƠN ĐĂNG KÝ
    // ==================================================================
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

        property.setWard(dto.getWard());
        property.setProvinceCode(dto.getProvinceCode());
        property.setDistrictCode(dto.getDistrictCode());

        // Contact info
        property.setPhoneContact(owner.getPhoneNumber());
        property.setEmailContact(owner.getEmail());

        // Giá trị mặc định
        property.setPostalCode("70000");
        property.setLatitude(dto.getLatitude() != null ? dto.getLatitude() : BigDecimal.ZERO);
        property.setLongitude(dto.getLongitude() != null ? dto.getLongitude() : BigDecimal.ZERO);
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

        savedProperty.setPropertyDetail(detail);

        // 4. Xử lý Amenities
        if (dto.getAmenities() != null) {
            for (Map.Entry<String, Boolean> entry : dto.getAmenities().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    // Tìm Amenity trong DB
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
    // 2. LOGIC QUẢN LÝ TÀI SẢN (OWNER & ADMIN & SEARCH)
    // ==================================================================

    // ✅ HÀM MỚI: Lấy chi tiết Property kèm theo Amenities và Rooms
    @Override
    @Transactional(readOnly = true)
    public PropertyDetailDTO getPropertyDetailById(Integer id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        PropertyDetailDTO dto = mapToPropertyDetailDTO(property);

        // 1. Lấy ảnh PROPERTY (Header)
        List<String> propertyImages = propertyImageRepository.findByProperty_PropertyId(id)
                .stream()
                .map(PropertyImage::getImageUrl)
                .collect(Collectors.toList());
        dto.setImages(propertyImages);

        // 2. Lấy Amenities
        if (property.getPropertyAmenities() != null) {
            List<PropertyAmenityResponseDTO> amenities = property.getPropertyAmenities().stream()
                    .filter(PropertyAmenity::isActive)
                    .map(PropertyAmenityResponseDTO::new)
                    .collect(Collectors.toList());
            dto.setAmenities(amenities);
        }

        // 3. Lấy danh sách ROOMS & ẢNH ROOM (Fix lỗi không hiện ảnh bên dưới)
        if (property.getRooms() != null) {
            List<RoomResponseDTO> roomDTOs = new ArrayList<>();

            for (Room room : property.getRooms()) {
                if (room.isActive()) {
                    RoomResponseDTO roomDTO = new RoomResponseDTO(room);

                    // ✅ FETCH ẢNH PHÒNG TỪ DB
                    // Giả sử RoomImageRepository có hàm findByRoom_RoomId
                    List<String> roomImageUrls = roomImageRepository.findByRoom_RoomId(room.getRoomId())
                            .stream()
                            .map(RoomImage::getImageUrl)
                            .collect(Collectors.toList());

                    roomDTO.setImages(roomImageUrls);

                    roomDTOs.add(roomDTO);
                }
            }
            dto.setRooms(roomDTOs);
        } else {
            dto.setRooms(new ArrayList<>());
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getOwnerActiveProperties(String ownerId) {
        List<Property> properties = propertyRepository.findByOwner_UserIdAndPropertyStatus(ownerId, PropertyStatus.APPROVE);
        return properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getOwnerProperties(String ownerId) {
        List<Property> properties = propertyRepository.findAllByOwnerIdAndNotRejected(ownerId);
        return properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

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
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> searchProperties(String keyword, Integer guests) {
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }
        // Lấy list entity
        List<Property> properties = propertyRepository.searchProperties(keyword);

        // Chuyển sang DTO
        return properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PropertyDetailDTO updateProperty(int id, Property updatedProperty) {
        Property existingProperty = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + id));

        if (updatedProperty.getPropertyName() != null) existingProperty.setPropertyName(updatedProperty.getPropertyName());

        if (updatedProperty.getAddress() != null) existingProperty.setAddress(updatedProperty.getAddress());
        if (updatedProperty.getCity() != null) existingProperty.setCity(updatedProperty.getCity());
        if (updatedProperty.getProvince() != null) existingProperty.setProvince(updatedProperty.getProvince());
        if (updatedProperty.getCountry() != null) existingProperty.setCountry(updatedProperty.getCountry());

        if (updatedProperty.getWard() != null) existingProperty.setWard(updatedProperty.getWard());
        if (updatedProperty.getProvinceCode() != null) existingProperty.setProvinceCode(updatedProperty.getProvinceCode());
        if (updatedProperty.getDistrictCode() != null) existingProperty.setDistrictCode(updatedProperty.getDistrictCode());

            if (updatedProperty.getLatitude() != null) existingProperty.setLatitude(updatedProperty.getLatitude());
        if (updatedProperty.getLongitude() != null) existingProperty.setLongitude(updatedProperty.getLongitude());

        if (updatedProperty.getDescription() != null) existingProperty.setDescription(updatedProperty.getDescription());

        existingProperty.setUpdatedAt(LocalDate.now());

        Property savedProperty = propertyRepository.save(existingProperty);

        return mapToPropertyDetailDTO(savedProperty);
    }

    @Override
    public List<PropertyDetailDTO> getFeaturedProperties() {
        return propertyRepository.findFeaturedProperties().stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status) {
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

    // ==================================================================
    // 3. HELPER METHODS (QUAN TRỌNG)
    // ==================================================================

    // ✅ Helper map Entity -> DTO và tự động lấy ảnh bìa + tính giá
    private PropertyDetailDTO mapToPropertyDetailDTO(Property property) {
        PropertyDetailDTO dto = new PropertyDetailDTO(property);

        // 1. Logic lấy ảnh bìa (Cover Image)
        Optional<PropertyImage> coverImage = propertyImageRepository.findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId());
        if (coverImage.isPresent()) {
            dto.setCoverImage(coverImage.get().getImageUrl());
        } else {
            propertyImageRepository.findFirstByProperty_PropertyId(property.getPropertyId())
                    .ifPresent(img -> dto.setCoverImage(img.getImageUrl()));
        }

        // =================================================================
        // [QUAN TRỌNG] THÊM ĐOẠN NÀY ĐỂ TRẢ VỀ DANH SÁCH ẢNH
        // =================================================================
        // Lấy danh sách tất cả URL ảnh của property này
        if (property.getImages() != null && !property.getImages().isEmpty()) {
            List<String> allImages = property.getImages().stream()
                    .map(PropertyImage::getImageUrl)
                    .collect(Collectors.toList());
            dto.setImages(allImages);
        } else {
            // Fallback: Nếu entity property chưa fetch images, query trực tiếp từ repo
            List<String> allImages = propertyImageRepository.findByProperty_PropertyId(property.getPropertyId())
                    .stream()
                    .map(PropertyImage::getImageUrl)
                    .collect(Collectors.toList());
            dto.setImages(allImages);
        }

        // 2. Logic tính khoảng giá (Min - Max)
        if (property.getRooms() != null && !property.getRooms().isEmpty()) {
            List<BigDecimal> prices = property.getRooms().stream()
                    .filter(Room::isActive)
                    .map(Room::getPricePerNight)
                    .toList();

            if (!prices.isEmpty()) {
                BigDecimal min = prices.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
                BigDecimal max = prices.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

                dto.setMinPrice(min);
                dto.setMaxPrice(max);
            }
        }

        return dto;
    }

    private void validateProperty(Property property) {
        if (property.getAddress() == null || property.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Address cannot be empty");
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


    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> findNearbyProperties(Double lat, Double lng, Double radius) {
        if (lat == null || lng == null) {
            return new ArrayList<>();
        }

        // Mặc định tìm trong 10km nếu không truyền radius
        double searchRadius = (radius != null) ? radius : 10.0;

        // Gọi Repository
        List<Property> nearbyProperties = propertyRepository.findNearbyProperties(lat, lng, searchRadius);

        // Convert sang DTO dùng hàm helper có sẵn trong class này
        return nearbyProperties.stream()
                .map(this::mapToPropertyDetailDTO) // Tái sử dụng logic map ảnh và giá
                .collect(Collectors.toList());
    }
}