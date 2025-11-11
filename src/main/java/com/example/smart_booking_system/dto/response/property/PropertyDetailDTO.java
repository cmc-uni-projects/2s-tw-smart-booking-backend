package com.example.smart_booking_system.dto.response.property;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PropertyDetailDTO {
    // Lấy từ Property Entity
    private int propertyId;
    private String propertyName;
    private PropertyType propertyType;
    private String address;
    private String country;
    private String city;
    private String postalCode;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneContact;
    private String emailContact;
    private BigDecimal rating;
    private int reviewCount;
    private boolean isActive;
    private PropertyStatus propertyStatus;
    private LocalDate createdAt;

    // Lấy từ User (Owner)
    private String ownerId;
    private String ownerFullName;

    // Constructor (Hàm khởi tạo) này sẽ chuyển Entity -> DTO
    // Nó xử lý an toàn các trường LAZY (như OwnerId)
    public PropertyDetailDTO(Property property) {
        this.propertyId = property.getPropertId();
        this.propertyName = property.getPropertyName();
        this.propertyType = property.getPropertyType();
        this.address = property.getAddress();
        this.country = property.getCountry();
        this.city = property.getCity();
        this.postalCode = property.getPostalCode();
        this.description = property.getDescription();
        this.latitude = property.getLatitude();
        this.longitude = property.getLongitude();
        this.phoneContact = property.getPhoneContact();
        this.emailContact = property.getEmailContact();
        this.rating = property.getRating();
        this.reviewCount = property.getReviewCount();
        this.isActive = property.isActive();
        this.propertyStatus = property.getPropertyStatus();
        this.createdAt = property.getCreatedAt();

        // Xử lý an toàn liên kết LAZY (tránh lỗi ByteBuddy)
        if (property.getOwnerId() != null) {
            this.ownerId = property.getOwnerId().getUserId();
            this.ownerFullName = property.getOwnerId().getFullName();
        }
    }
}