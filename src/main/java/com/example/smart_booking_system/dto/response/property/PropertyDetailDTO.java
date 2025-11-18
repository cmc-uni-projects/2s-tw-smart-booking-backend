package com.example.smart_booking_system.dto.response.property;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.PropertyAmenityResponseDTO;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PropertyDetailDTO {
    private int propertyId;
    private String propertyName;
    private PropertyType propertyType;
    private String address;
    private String city;
    private String province;
    private String country;
    private String description;
    private BigDecimal rating;
    private int reviewCount;
    private PropertyStatus propertyStatus;
    private String coverImage;
    private String ownerId;
    private String ownerName;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private List<String> images;
    private List<PropertyAmenityResponseDTO> amenities;
    private List<RoomResponseDTO> rooms;

    public PropertyDetailDTO(Property property) {
        this.propertyId = property.getPropertyId();
        this.propertyName = property.getPropertyName();
        this.propertyType = property.getPropertyType();
        this.address = property.getAddress();
        this.city = property.getCity();
        this.province = property.getProvince();
        this.country = property.getCountry();
        this.description = property.getDescription();
        this.rating = property.getRating();
        this.reviewCount = property.getReviewCount();
        this.propertyStatus = property.getPropertyStatus();

        if (property.getOwner() != null) {
            this.ownerId = property.getOwner().getUserId();
            this.ownerName = property.getOwner().getFullName();
        }
    }
}