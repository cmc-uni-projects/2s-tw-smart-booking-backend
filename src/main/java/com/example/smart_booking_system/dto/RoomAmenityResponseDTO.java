package com.example.smart_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class RoomAmenityResponseDTO {
    private int roomAmenityId;
    private int roomId;
    private int amenityId;
    private String amenityName;
}
