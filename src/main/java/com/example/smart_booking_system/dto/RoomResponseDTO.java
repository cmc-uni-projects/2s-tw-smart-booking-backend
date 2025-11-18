package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.entity.Room; // ✅ Import Entity
import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList;
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

    public RoomResponseDTO(Room room) {
        this.roomId = room.getRoomId();
        if (room.getPropertyId() != null) {
            this.propertyId = room.getPropertyId().getPropertyId();
        }
        this.roomName = room.getRoomName();
        this.roomCategory = room.getRoomCategory();
        this.pricePerNight = room.getPricePerNight();
        this.capacity = room.getCapacity();
        this.description = room.getDescription();
        this.roomStatus = room.getRoomStatus();
        this.active = room.isActive();
        this.images = new ArrayList<>();
        this.amenities = new ArrayList<>();
    }
}