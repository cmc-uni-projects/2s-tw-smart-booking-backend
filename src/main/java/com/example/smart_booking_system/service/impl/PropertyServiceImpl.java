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
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;
import com.example.smart_booking_system.dto.response.property.PropertyMapDTO;
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
    private final BookingRepository bookingRepository;
    private final RoomAmenityRepository roomAmenityRepository;

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
    public PropertyDetailDTO getPropertyDetailById(Integer id, LocalDate checkIn, LocalDate checkOut) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        PropertyDetailDTO dto = mapToPropertyDetailDTO(property);

        // 1. Lấy ảnh Property (Giữ nguyên)
        List<String> propertyImages = propertyImageRepository.findByProperty_PropertyId(id)
                .stream()
                .map(PropertyImage::getImageUrl)
                .collect(Collectors.toList());
        dto.setImages(propertyImages);

        // 2. Lấy Amenities (Giữ nguyên)
        if (property.getPropertyAmenities() != null) {
            List<PropertyAmenityResponseDTO> amenities = property.getPropertyAmenities().stream()
                    .filter(PropertyAmenity::isActive)
                    .map(PropertyAmenityResponseDTO::new)
                    .collect(Collectors.toList());
            dto.setAmenities(amenities);
        }

        // 3. Lấy danh sách ROOMS & LỌC PHÒNG ĐÃ ĐẶT
        if (property.getRooms() != null) {
            List<RoomResponseDTO> roomDTOs = new ArrayList<>();

            for (Room room : property.getRooms()) {
                // Chỉ lấy phòng đang hoạt động
                if (room.isActive()) {

                    // ✅ LOGIC MỚI: Kiểm tra lịch trống nếu có ngày check-in/out
                    if (checkIn != null && checkOut != null) {
                        List<Booking> overlaps = bookingRepository.findConfirmedOverlappingByRoomId(
                                room.getRoomId(), checkIn, checkOut);

                        // Nếu có booking trùng -> Bỏ qua phòng này (không add vào list)
                        if (!overlaps.isEmpty()) {
                            continue;
                        }
                    }

                    // Map Room Entity -> DTO
                    RoomResponseDTO roomDTO = new RoomResponseDTO(room);

                    // Lấy ảnh phòng
                    List<String> roomImageUrls = roomImageRepository.findByRoom_RoomId(room.getRoomId())
                            .stream()
                            .map(RoomImage::getImageUrl)
                            .collect(Collectors.toList());
                    roomDTO.setImages(roomImageUrls);

                    // Lấy tiện nghi phòng
                    List<String> roomAmenities = roomAmenityRepository.findByRoom_RoomId(room.getRoomId())
                            .stream()
                            .filter(RoomAmenity::isActive)
                            .map(ra -> ra.getAmenity().getAmenityName())
                            .collect(Collectors.toList());
                    roomDTO.setAmenities(roomAmenities);

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
    public List<PropertyDetailDTO> searchProperties(String keyword, Integer guests, LocalDate checkIn, LocalDate checkOut) {
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        // 1. Tìm các Property có ít nhất 1 phòng thỏa mãn điều kiện (Query DB)
        List<Property> properties = propertyRepository.searchProperties(keyword, guests, checkIn, checkOut);

        // 2. Map sang DTO đồng thời lọc bỏ các phòng đã đặt bên trong DTO
        return properties.stream()
                .map(p -> mapToPropertyDetailDTOWithFilter(p, guests, checkIn, checkOut))
                .collect(Collectors.toList());
    }

    private PropertyDetailDTO mapToPropertyDetailDTOWithFilter(Property property, Integer guests, LocalDate checkIn, LocalDate checkOut) {
        // Gọi hàm map cơ bản có sẵn
        PropertyDetailDTO dto = mapToPropertyDetailDTO(property);

        // Lọc danh sách phòng bên trong DTO
        if (dto.getRooms() != null) {
            List<RoomResponseDTO> filteredRooms = dto.getRooms().stream()
                    .filter(roomDTO -> {
                        // 1. Lọc theo sức chứa
                        if (guests != null && roomDTO.getCapacity() < guests) return false;

                        // 2. Lọc theo ngày trống (Check booking trùng)
                        if (checkIn != null && checkOut != null) {
                            List<Booking> overlaps = bookingRepository.findConfirmedOverlappingByRoomId(
                                    roomDTO.getRoomId(), checkIn, checkOut);
                            // Nếu có booking trùng -> loại bỏ phòng này
                            if (!overlaps.isEmpty()) return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            dto.setRooms(filteredRooms);

            // Tính lại giá Min/Max dựa trên các phòng còn trống
            if (!filteredRooms.isEmpty()) {
                BigDecimal min = filteredRooms.stream().map(RoomResponseDTO::getPricePerNight).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
                BigDecimal max = filteredRooms.stream().map(RoomResponseDTO::getPricePerNight).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
                dto.setMinPrice(min);
                dto.setMaxPrice(max);
            } else {
                // Trường hợp hãn hữu: Query DB bảo có phòng, nhưng check lại thì hết (race condition hoặc logic vênh)
                dto.setMinPrice(BigDecimal.ZERO);
                dto.setMaxPrice(BigDecimal.ZERO);
            }
        }
        return dto;
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
        // 1. Lấy admin
        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        // 2. Lấy property
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        // 3. Kiểm tra trạng thái hiện tại
        if (property.getPropertyStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Property already reviewed");
        }

        // 4. Kiểm tra trạng thái hợp lệ
        if (!reviewDTO.isValidForReview()) {
            throw new IllegalArgumentException("Chỉ được phép APPROVE hoặc REJECTED.");
        }

        // 5. Cập nhật trạng thái property
        PropertyStatus newStatus = reviewDTO.getStatus();
        property.setPropertyStatus(newStatus);
        property.setUpdatedAt(LocalDate.now());
        property.setActive(newStatus == PropertyStatus.APPROVE);

        Property savedProperty = propertyRepository.save(property);

        // 6. Gửi email thông báo cho owner
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
    public List<PropertyMapDTO> findNearbyProperties(Double lat, Double lng, Double radius) { // Đổi kiểu trả về
        if (lat == null || lng == null) return new ArrayList<>();

        double searchRadius = (radius != null) ? radius : 10.0;
        List<Property> nearbyProperties = propertyRepository.findNearbyProperties(lat, lng, searchRadius);

        return nearbyProperties.stream().map(property -> {
            // 1. Lấy ảnh bìa nhanh gọn
            String cover = propertyImageRepository.findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                    .map(img -> img.getImageUrl())
                    .orElse(null);

            // 2. Tính giá thấp nhất (Min Price)
            BigDecimal minPrice = BigDecimal.ZERO;
            if (property.getRooms() != null && !property.getRooms().isEmpty()) {
                minPrice = property.getRooms().stream()
                        .map(Room::getPricePerNight)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
            }

            return new PropertyMapDTO(property, cover, minPrice);
        }).collect(Collectors.toList());
    }
}