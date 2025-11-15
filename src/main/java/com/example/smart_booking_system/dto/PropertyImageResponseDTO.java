package com.example.smart_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PropertyImageResponseDTO {
    private int propertyImageId;
    private int propertyId;
    private String imageUrl;
    private boolean active;
}
