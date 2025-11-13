package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import com.example.smart_booking_system.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class  PropertyService {
    private final PropertyRepository propertyRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    public Property addProperty(Property property, String ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner (User) not found with ID: " + ownerId));

        // 2. Gán Owner vào Property
        property.setOwnerId(owner);

        // Validate address
        if (property.getAddress() == null || property.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Address cannot be empty");
        }

        // Validate postal code
        if (property.getPostalCode() == null || property.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code cannot be empty");
        }

        // Validate latitude
        if (property.getLatitude().compareTo(BigDecimal.valueOf(-90.0)) < 0 ||
                property.getLatitude().compareTo(BigDecimal.valueOf(90.0)) > 0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }

        // Validate longitude
        if (property.getLongitude().compareTo(BigDecimal.valueOf(-180.0)) < 0 ||
                property.getLongitude().compareTo(BigDecimal.valueOf(180.0)) > 0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }

        // Validate phone number
        if (!property.getPhoneContact().matches("^[0-9]{10}$")) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits with no special characters");
        }

        // Set default status if not provided
        if (property.getPropertyStatus() == null) {
            property.setPropertyStatus(PropertyStatus.PENDING);
        }

        Property savedProperty = propertyRepository.save(property);

        if (owner != null) {
            try {
                sendPropertySubmittedEmail(owner, savedProperty);
            } catch (Exception e) {
                System.err.println("Lỗi gửi email xác nhận cho Owner: " + e.getMessage());
            }
        }
        return savedProperty;
    }

    public List<Property> searchProperties(String city, String keyword) {
        if (city != null && city.trim().isEmpty()) city = null;
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;

        return propertyRepository.searchProperties(city, keyword);
    }

    public Property updateProperty(int id, Property updatedProperty) {
        Property existingProperty = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with id: " + id));

        if (updatedProperty.getPropertyName() != null &&
                !updatedProperty.getPropertyName().equals(existingProperty.getPropertyName())) {
            existingProperty.setPropertyName(updatedProperty.getPropertyName());
        }

        if (updatedProperty.getAddress() != null &&
                !updatedProperty.getAddress().equals(existingProperty.getAddress())) {
            existingProperty.setAddress(updatedProperty.getAddress());
        }

        if (updatedProperty.getCity() != null &&
                !updatedProperty.getCity().equals(existingProperty.getCity())) {
            existingProperty.setCity(updatedProperty.getCity());
        }

        if (updatedProperty.getCountry() != null &&
                !updatedProperty.getCountry().equals(existingProperty.getCountry())) {
            existingProperty.setCountry(updatedProperty.getCountry());
        }

        if (updatedProperty.getPostalCode() != null &&
                !updatedProperty.getPostalCode().equals(existingProperty.getPostalCode())) {
            existingProperty.setPostalCode(updatedProperty.getPostalCode());
        }

        if (updatedProperty.getDescription() != null &&
                !updatedProperty.getDescription().equals(existingProperty.getDescription())) {
            existingProperty.setDescription(updatedProperty.getDescription());
        }

        if (updatedProperty.getLatitude() != null &&
                !updatedProperty.getLatitude().equals(existingProperty.getLatitude())) {
            existingProperty.setLatitude(updatedProperty.getLatitude());
        }

        if (updatedProperty.getLongitude() != null &&
                !updatedProperty.getLongitude().equals(existingProperty.getLongitude())) {
            existingProperty.setLongitude(updatedProperty.getLongitude());
        }

        if (updatedProperty.getPhoneContact() != null &&
                !updatedProperty.getPhoneContact().equals(existingProperty.getPhoneContact())) {
            existingProperty.setPhoneContact(updatedProperty.getPhoneContact());
        }

        if (updatedProperty.getEmailContact() != null &&
                !updatedProperty.getEmailContact().equals(existingProperty.getEmailContact())) {
            existingProperty.setEmailContact(updatedProperty.getEmailContact());
        }

        if (updatedProperty.getPropertyType() != null &&
                !updatedProperty.getPropertyType().equals(existingProperty.getPropertyType())) {
            existingProperty.setPropertyType(updatedProperty.getPropertyType());
        }

        if (updatedProperty.isActive() != existingProperty.isActive()) {
            existingProperty.setActive(updatedProperty.isActive());
        }

        existingProperty.setUpdatedAt(LocalDate.now());

        return propertyRepository.save(existingProperty);
    }


    public List<PropertyDetailDTO> getFeaturedProperties() {

        List<Property> properties = propertyRepository.findFeaturedProperties();

        return properties.stream()
                .map(this::convertToFeaturedDTO)
                .collect(Collectors.toList());
    }

    private PropertyDetailDTO convertToFeaturedDTO(Property property) {
        PropertyDetailDTO dto = new PropertyDetailDTO();
        dto.setPropertyId(property.getPropertyId());
        dto.setPropertyName(property.getPropertyName());
        dto.setCity(property.getCity());
        dto.setRating(property.getRating());
        dto.setReviewCount(property.getReviewCount());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status) {
        List<Property> properties = propertyRepository.findByPropertyStatus(status);


        return properties.stream()
                .map(PropertyDetailDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public PropertyDetailDTO reviewProperty(Integer propertyId, PropertyReviewDTO reviewDTO, String adminUsername) {


        User admin = userRepository.findByEmail(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminUsername));


        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Property với ID: " + propertyId));

        if (property.getPropertyStatus() != PropertyStatus.PENDING) {
            throw new IllegalStateException("Cơ sở này đã được xử lý (duyệt hoặc từ chối) trước đó.");
        }

        PropertyStatus newStatus = PropertyStatus.valueOf(reviewDTO.getStatus().toUpperCase());
        property.setPropertyStatus(newStatus);
        property.setUpdatedAt(LocalDate.now());

        if (newStatus == PropertyStatus.APPROVE) {
            property.setActive(true);
        } else if (newStatus == PropertyStatus.REJECTED) {
            property.setActive(false);
        }

        Property savedProperty = propertyRepository.save(property);
        User owner = property.getOwnerId();

        if (owner != null) {
            sendPropertyReviewEmail(owner, savedProperty, reviewDTO.getReason());
        }

        return new PropertyDetailDTO(savedProperty);
    }

    private void sendPropertyReviewEmail(User owner, Property property, String reason) {
        String ownerEmail = owner.getEmail();
        String ownerName = owner.getFullName();
        String propertyName = property.getPropertyName();

        PropertyStatus status = property.getPropertyStatus();

        Context context = new Context();
        context.setVariable("ownerName", ownerName);
        context.setVariable("propertyName", propertyName);
        context.setVariable("reason", (reason != null && !reason.isEmpty()) ? reason : "N/A");

        String subject;
        String templateName;

        if (status == PropertyStatus.APPROVE) {
            subject = "Chúc mừng! Cơ sở " + propertyName + " của bạn đã được DUYỆT";
            templateName = "email/property-approved";
        } else if (status == PropertyStatus.REJECTED) {
            subject = "Thông báo: Cơ sở " + propertyName + " của bạn đã bị TỪ CHỐI";
            templateName = "email/property-rejected";
        } else {
            return;
        }

        emailService.sendHtmlEmail(ownerEmail, subject, templateName, context);
    }
    private void sendPropertySubmittedEmail(User owner, Property property) {
        String subject = "Xác nhận: Đã nhận được đơn đăng ký cơ sở " + property.getPropertyName();
        String templateName = "email/property-submitted-confirmation";

        Context context = new Context();
        context.setVariable("ownerName", owner.getFullName());
        context.setVariable("propertyName", property.getPropertyName());

        emailService.sendHtmlEmail(owner.getEmail(), subject, templateName, context);
    }
}
