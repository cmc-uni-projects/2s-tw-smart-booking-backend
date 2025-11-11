package com.example.smart_booking_system.mapper;

import com.example.smart_booking_system.dto.RoomTypeResponse;
import com.example.smart_booking_system.entity.RoomType;

public class RoomTypeMapper {

    public static RoomTypeResponse toResponse(RoomType entity) {
        RoomTypeResponse dto = new RoomTypeResponse();
        dto.setRoomTypeId(entity.getRoomTypeId());
        dto.setRoomTypeName(entity.getRoomTypeName());
        dto.setDescription(entity.getDescription());
        dto.setPricePerNight(entity.getPricePerNight());
        dto.setCapacity(entity.getCapacity());
        dto.setPolicy(entity.getPolicy());

        if (entity.getPropertyId() != null) {
            dto.setPropertyId(entity.getPropertyId().getPropertyId());
            dto.setPropertyName(entity.getPropertyId().getPropertyName());
        }

        return dto;
    }
}
