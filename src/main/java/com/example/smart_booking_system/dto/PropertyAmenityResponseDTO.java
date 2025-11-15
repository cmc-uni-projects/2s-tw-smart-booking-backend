package com.example.smart_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAmenityResponseDTO {
    private int propertyAmenityId;
    private int propertyId;
    private int amenityId;
    private String amenityName;
}
