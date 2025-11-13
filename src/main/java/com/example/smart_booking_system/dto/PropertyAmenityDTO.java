package com.example.smart_booking_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PropertyAmenityDTO {
    private int propertyAmenityId;
    private int propertyId;
    private int amenityId;
    private String amenityName;
}
