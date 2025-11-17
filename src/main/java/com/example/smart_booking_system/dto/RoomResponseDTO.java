package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDTO {
    private int roomId;
    private int propertyId;
    private String roomName;
    private RoomCategory roomCategory;
    private BigDecimal pricePerNight;
    private int capacity;
    private String description;
    private RoomStatus roomStatus;
    private boolean active;

    private List<String> images;
    private List<String> amenities;
}