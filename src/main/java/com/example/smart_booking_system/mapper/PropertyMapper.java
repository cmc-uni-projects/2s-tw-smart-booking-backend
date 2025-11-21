package com.example.smart_booking_system.mapper;

import com.example.smart_booking_system.dto.PropertySimpleDTO;
import com.example.smart_booking_system.entity.Property;

public class PropertyMapper {

    public static PropertySimpleDTO toSimpleDTO(Property property) {
        PropertySimpleDTO dto = new PropertySimpleDTO();
        dto.setPropertyId(property.getPropertyId());
        dto.setPropertyName(property.getPropertyName());
        dto.setRating(property.getRating());
        dto.setReviewCount(property.getReviewCount());
        dto.setCity(property.getCity());
        dto.setPropertyType(property.getPropertyType().name());
        return dto;
    }
}
