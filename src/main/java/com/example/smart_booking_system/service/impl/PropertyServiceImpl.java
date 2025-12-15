package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;
import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.dto.response.property.PropertyMapDTO;
import com.example.smart_booking_system.dto.PropertyResponseDTO;
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.enums.AmenityType;
import com.example.smart_booking_system.enums.NotificationType;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.exception.ForbiddenException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.*;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.FileStorageService;
import com.example.smart_booking_system.service.NotificationService;
import com.example.smart_booking_system.service.PropertyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final PropertyDetailRepository propertyDetailRepository;
    private final AmenityRepository amenityRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final FileStorageService fileStorageService;
    private final RoomImageRepository roomImageRepository;
    private final BookingRepository bookingRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    // ============================================================
    // VALIDATION
    // ============================================================
    private void validateProperty(Property property) {
        if (property.getAddress() == null || property.getAddress().trim().isEmpty())
            throw new IllegalArgumentException("Address cannot be empty");
    }

    // ============================================================
    // ADD PROPERTY
    // ============================================================
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
        } catch (Exception ignored) {}

        return savedProperty;
    }

    // ============================================================
    // UPDATE PROPERTY
    // ============================================================
    @Override
    public PropertyDetailDTO updateProperty(int id, Property updatedProperty) {

        Property existing = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + id));

        if (updatedProperty.getPropertyName() != null)
            existing.setPropertyName(updatedProperty.getPropertyName());

        if (updatedProperty.getAddress() != null)
            existing.setAddress(updatedProperty.getAddress());

        if (updatedProperty.getCity() != null)
            existing.setDistrict(updatedProperty.getCity());

        if (updatedProperty.getProvince() != null) {
            existing.setCity(updatedProperty.getProvince());
            existing.setProvince(updatedProperty.getProvince());
        }

        if (updatedProperty.getCountry() != null)
            existing.setCountry(updatedProperty.getCountry());

        if (updatedProperty.getWard() != null)
            existing.setWard(updatedProperty.getWard());

        if (updatedProperty.getProvinceCode() != null)
            existing.setProvinceCode(updatedProperty.getProvinceCode());

        if (updatedProperty.getDistrictCode() != null)
            existing.setDistrictCode(updatedProperty.getDistrictCode());

        if (updatedProperty.getLatitude() != null)
            existing.setLatitude(updatedProperty.getLatitude());

        if (updatedProperty.getLongitude() != null)
            existing.setLongitude(updatedProperty.getLongitude());

        if (updatedProperty.getDescription() != null)
            existing.setDescription(updatedProperty.getDescription());

        existing.setUpdatedAt(LocalDate.now());

        return mapToPropertyDetailDTO(propertyRepository.save(existing));
    }

    // ============================================================
    // 1. SUBMIT APPLICATION
    // ============================================================
    @Override
    public PropertyDetailDTO submitPropertyApplication(PropertyApplicationSubmitDTO dto,
                                                       List<MultipartFile> images,
                                                       String ownerId) {

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + ownerId));

        Property property = new Property();
        property.setOwner(owner);
        property.setPropertyName(dto.getPropertyName());
        property.setDescription(dto.getDescription());
        property.setPropertyType(dto.getPropertyType());
        property.setAddress(dto.getAddress());
        property.setCity(dto.getProvince());
        property.setProvince(dto.getProvince());
        property.setCountry(dto.getCountry());
        property.setDistrict(dto.getCity());
        property.setWard(dto.getWard());
        property.setProvinceCode(dto.getProvinceCode());
        property.setDistrictCode(dto.getDistrictCode());

        property.setPhoneContact(owner.getPhoneNumber());
        property.setEmailContact(owner.getEmail());
        property.setPostalCode("70000");

        property.setLatitude(dto.getLatitude() != null ? dto.getLatitude() : BigDecimal.ZERO);
        property.setLongitude(dto.getLongitude() != null ? dto.getLongitude() : BigDecimal.ZERO);

        property.setPropertyStatus(PropertyStatus.PENDING);
        property.setActive(false);

        property.setCreatedAt(LocalDate.now());
        property.setUpdatedAt(LocalDate.now());

        Property saved = propertyRepository.save(property);

        // DETAIL
        PropertyDetail detail = new PropertyDetail();
        detail.setProperty(saved);
        detail.setArea(dto.getArea());
        propertyDetailRepository.save(detail);
        saved.setPropertyDetail(detail);

        // AMENITIES
        if (dto.getAmenities() != null) {
            dto.getAmenities().forEach((name, enabled) -> {
                if (Boolean.TRUE.equals(enabled)) {
                    Amenity am = amenityRepository
                            .findByAmenityNameAndAmenityType(name, AmenityType.PROPERTY)
                            .orElse(null);
                    if (am != null) {
                        PropertyAmenity pa = new PropertyAmenity();
                        pa.setProperty(saved);
                        pa.setAmenity(am);
                        propertyAmenityRepository.save(pa);
                    }
                }
            });
        }

        // IMAGES
        if (images != null && !images.isEmpty()) {
            String folder = "properties/" + saved.getPropertyId();

            for (int i = 0; i < images.size(); i++) {
                String key = fileStorageService.storeImageFile(images.get(i), folder);

                PropertyImage img = new PropertyImage();
                img.setProperty(saved);
                img.setImageUrl(key);
                img.setCover(i == 0);

                propertyImageRepository.save(img);
            }
        }

        try {
            sendPropertySubmittedEmail(owner, saved);
        } catch (Exception ignored) {}

        return mapToPropertyDetailDTO(saved);
    }

    @Override
    public boolean checkNameAvailability(String propertyName) {
        return !propertyRepository.existsByPropertyName(propertyName);
    }

    // ============================================================
    // GET PROPERTY DETAIL (SIGNED URL)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public PropertyDetailDTO getPropertyDetailById(Integer id, LocalDate checkIn, LocalDate checkOut) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        PropertyDetailDTO dto = mapToPropertyDetailDTO(property);

        // ALL IMAGES signed
        List<String> signedImages = propertyImageRepository.findByProperty_PropertyId(id)
                .stream()
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .collect(Collectors.toList());

        dto.setImages(signedImages);

        // AMENITIES
        if (property.getPropertyAmenities() != null) {
            dto.setAmenities(
                    property.getPropertyAmenities().stream()
                            .filter(PropertyAmenity::isActive)
                            .map(PropertyAmenityResponseDTO::new)
                            .collect(Collectors.toList())
            );
        }

        // ROOMS
        List<RoomResponseDTO> rooms = new ArrayList<>();

        if (property.getRooms() != null) {
            for (Room room : property.getRooms()) {
                if (!room.isActive()) continue;

                if (checkIn != null && checkOut != null) {
                    boolean booked = !bookingRepository
                            .findConfirmedOverlappingByRoomId(room.getRoomId(), checkIn, checkOut)
                            .isEmpty();
                    if (booked) continue;
                }

                RoomResponseDTO rDto = new RoomResponseDTO(room);

                List<String> roomImages = roomImageRepository.findByRoom_RoomId(room.getRoomId())
                        .stream()
                        .map(RoomImage::getImageUrl)
                        .map(fileStorageService::generateSignedUrl)
                        .collect(Collectors.toList());
                rDto.setImages(roomImages);

                List<String> ams = roomAmenityRepository.findByRoom_RoomId(room.getRoomId())
                        .stream()
                        .filter(RoomAmenity::isActive)
                        .map(ra -> ra.getAmenity().getAmenityName())
                        .collect(Collectors.toList());
                rDto.setAmenities(ams);

                rooms.add(rDto);
            }
        }

        dto.setRooms(rooms);

        return dto;
    }

    // ============================================================
    // SEARCH (list view → PropertyResponseDTO)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> searchProperties(String keyword,
                                                    Integer guests,
                                                    LocalDate checkIn,
                                                    LocalDate checkOut) {

        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        List<Property> properties = propertyRepository.searchProperties(keyword, guests, checkIn, checkOut);

        return properties.stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // OWNER PROPERTIES (list → use PropertyDetailDTO)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getOwnerProperties(String ownerId) {
        return propertyRepository.findAllByOwnerIdAndNotRejected(ownerId)
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getOwnerActiveProperties(String ownerId) {
        return propertyRepository.findByOwner_UserIdAndPropertyStatus(ownerId, PropertyStatus.APPROVE)
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // FEATURED (list → use PropertyResponseDTO)
    // ============================================================
    @Override
    public List<PropertyDetailDTO> getFeaturedProperties() {
        return propertyRepository.findFeaturedProperties()
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // STATUS FILTER (ADMIN LIST → NEED SIGNED URL COVER)
    // ============================================================
    @Override
    public List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status) {
        return propertyRepository.findByPropertyStatus(status)
                .stream()
                .map(this::mapToPropertyDetailDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // REVIEW
    // ============================================================
    @Override
    public PropertyDetailDTO reviewProperty(Integer propertyId, PropertyReviewDTO reviewDTO, String adminUsername) {

        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        if (property.getPropertyStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Property already reviewed");
        }

        if (!reviewDTO.isValidForReview()) {
            throw new IllegalArgumentException("Chỉ được APPROVE hoặc REJECTED.");
        }

        PropertyStatus newStatus = reviewDTO.getStatus();

        property.setPropertyStatus(newStatus);
        property.setUpdatedAt(LocalDate.now());
        property.setActive(newStatus == PropertyStatus.APPROVE);

        Property saved = propertyRepository.save(property);

        if (property.getOwner() != null) {
            sendPropertyReviewEmail(property.getOwner(), saved, reviewDTO.getReason());
        }

        return mapToPropertyDetailDTO(saved);
    }

    // ============================================================
    // MAPPER: DETAIL DTO (cover + images signed)
    // ============================================================
    private PropertyDetailDTO mapToPropertyDetailDTO(Property property) {

        PropertyDetailDTO dto = new PropertyDetailDTO(property);

        // cover image signed
        propertyImageRepository.findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                .ifPresentOrElse(
                        img -> dto.setCoverImage(fileStorageService.generateSignedUrl(img.getImageUrl())),
                        () -> propertyImageRepository.findFirstByProperty_PropertyId(property.getPropertyId())
                                .ifPresent(img -> dto.setCoverImage(fileStorageService.generateSignedUrl(img.getImageUrl())))
                );

        // list images signed
        List<String> signed = propertyImageRepository.findByProperty_PropertyId(property.getPropertyId())
                .stream()
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .collect(Collectors.toList());
        dto.setImages(signed);

        // price range
        if (property.getRooms() != null && !property.getRooms().isEmpty()) {
            List<BigDecimal> prices = property.getRooms().stream()
                    .filter(Room::isActive)
                    .map(Room::getPricePerNight)
                    .toList();
            if (!prices.isEmpty()) {
                dto.setMinPrice(prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                dto.setMaxPrice(prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
            }
        }

        return dto;
    }

    // ============================================================
    // MAPPER: LIST VIEW → PropertyResponseDTO (SIGNED COVER)
    // ============================================================
    private PropertyResponseDTO mapToPropertyResponseDTO(Property property) {

        PropertyResponseDTO dto = new PropertyResponseDTO();

        dto.setPropertyId(property.getPropertyId());
        dto.setPropertyName(property.getPropertyName());
        dto.setPropertyType(property.getPropertyType());
        dto.setAddress(property.getAddress());
        dto.setCountry(property.getCountry());
        dto.setProvince(property.getProvince());
        dto.setCity(property.getCity());
        dto.setWard(property.getWard());
        dto.setProvinceCode(property.getProvinceCode());
        dto.setDistrictCode(property.getDistrictCode());
        dto.setPostalCode(property.getPostalCode());
        dto.setDescription(property.getDescription());
        dto.setPhoneContact(property.getPhoneContact());
        dto.setEmailContact(property.getEmailContact());
        dto.setLatitude(property.getLatitude());
        dto.setLongitude(property.getLongitude());
        dto.setCreatedAt(property.getCreatedAt());
        dto.setUpdatedAt(property.getUpdatedAt());
        dto.setPropertyStatus(property.getPropertyStatus());
        dto.setActive(property.isActive());

        // Signed cover
        String cover = propertyImageRepository
                .findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                .map(PropertyImage::getImageUrl)
                .map(fileStorageService::generateSignedUrl)
                .orElse(null);

        dto.setCoverImage(cover);

        return dto;
    }

    // ============================================================
    // NEARBY (signed cover)
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public List<PropertyMapDTO> findNearbyProperties(Double lat, Double lng, Double radius) {

        if (lat == null || lng == null) return new ArrayList<>();

        double range = radius != null ? radius : 10.0;

        return propertyRepository.findNearbyProperties(lat, lng, range)
                .stream()
                .map(property -> {

                    String cover = propertyImageRepository
                            .findFirstByProperty_PropertyIdAndIsCoverTrue(property.getPropertyId())
                            .map(PropertyImage::getImageUrl)
                            .map(fileStorageService::generateSignedUrl)
                            .orElse(null);

                    BigDecimal minPrice = property.getRooms() != null
                            ? property.getRooms().stream()
                            .map(Room::getPricePerNight)
                            .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO)
                            : BigDecimal.ZERO;

                    return new PropertyMapDTO(property, cover, minPrice);
                })
                .collect(Collectors.toList());
    }

    // ============================================================
    // EMAIL HELPERS
    // ============================================================
    private void sendPropertySubmittedEmail(User owner, Property property) {
        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());

        emailService.sendHtmlEmail(
                owner.getEmail(),
                "Xác nhận nộp đơn",
                "email/property-submitted-confirmation",
                context
        );
    }

    private void sendPropertyReviewEmail(User owner, Property property, String reason) {
        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());
        context.setVariable("reason", reason != null ? reason : "N/A");

        String template = property.getPropertyStatus() == PropertyStatus.APPROVE
                ? "email/property-approved"
                : "email/property-rejected";

        String subject = property.getPropertyStatus() == PropertyStatus.APPROVE
                ? "Cơ sở được duyệt"
                : "Cơ sở bị từ chối";

        emailService.sendHtmlEmail(owner.getEmail(), subject, template, context);
    }

    @Override
    public boolean togglePropertyStatus(Integer propertyId, String ownerId) {
        // 1. Tìm Property theo ID
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở lưu trú"));

        // 2. Kiểm tra quyền sở hữu (Owner phải là người sở hữu property này)
        if (!property.getOwner().getUserId().equals(ownerId)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa cơ sở này");
        }

        // 3. Kiểm tra trạng thái duyệt
        if (property.getPropertyStatus() != PropertyStatus.APPROVE) {
            throw new IllegalArgumentException("Cơ sở phải được Admin duyệt (APPROVE) mới có thể thay đổi trạng thái hoạt động.");
        }

        // 4. Đảo ngược trạng thái active
        boolean newStatus = !property.isActive();
        property.setActive(newStatus);
        property.setUpdatedAt(LocalDate.now());

        propertyRepository.save(property);

        return newStatus;
    }
    // ============================================================
    // [NEW] SUSPEND PROPERTY (ADMIN)
    // ============================================================
    @Override
    public void suspendProperty(Integer propertyId, String reason) {
        // 1. Tìm Property
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở lưu trú với ID: " + propertyId));

        // 2. Cập nhật trạng thái
        property.setPropertyStatus(PropertyStatus.SUSPENDED);
        property.setActive(false);
        propertyRepository.save(property);

        // 3. Lấy chủ sở hữu
        User owner = property.getOwner();

        // 4. Gửi Notification
        // ✅ FIX LỖI: Thêm tham số thứ 5 là String.valueOf(propertyId)
        notificationService.sendNotification(
                owner.getUserId(),
                "Cơ sở lưu trú bị tạm dừng hoạt động",
                "Cơ sở '" + property.getPropertyName() + "' đã bị admin tạm dừng. Lý do: " + reason,
                NotificationType.PROPERTY_SUSPENDED,
                String.valueOf(property.getPropertyId()) // <--- THAM SỐ CÒN THIẾU
        );

        // 5. Gửi Email
        emailService.sendPropertySuspensionEmail(
                owner.getEmail(),
                owner.getFullName(),
                property.getPropertyName(),
                reason
        );
    }

    // ============================================================
    // [NEW] GET ALL ACTIVE PROPERTIES (ADMIN)
    // ============================================================
    @Override
    public Page<PropertyResponseDTO> getAllActiveProperties(Pageable pageable) {
        // ✅ FIX LỖI: Repository đã có hàm nhận Pageable
        return propertyRepository.findByPropertyStatus(PropertyStatus.APPROVE, pageable)
                .map(this::mapToPropertyResponseDTO);
    }
}
