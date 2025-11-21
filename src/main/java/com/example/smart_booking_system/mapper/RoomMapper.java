package com.example.smart_booking_system.mapper;

import com.example.smart_booking_system.dto.RoomSimpleDTO;
import com.example.smart_booking_system.entity.Room;

public class RoomMapper {

    public static RoomSimpleDTO toSimpleDTO(Room r) {
        RoomSimpleDTO dto = new RoomSimpleDTO();
        dto.setRoomId(r.getRoomId());
        dto.setRoomName(r.getRoomName());
        dto.setCapacity(r.getCapacity());
        dto.setPricePerNight(r.getPricePerNight());
        dto.setRoomStatus(r.getRoomStatus().name());
        dto.setActive(r.isActive());
        return dto;
    }
}
