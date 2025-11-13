package com.example.smart_booking_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomAmenityDTO {
    private int roomAmenityId;
    private int roomId;
    private int amenityId;
    private String amenityName;
}
