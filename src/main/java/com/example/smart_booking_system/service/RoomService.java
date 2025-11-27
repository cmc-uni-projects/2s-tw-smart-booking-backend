package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomService {

    List<RoomResponseDTO> getRoomsByPropertyId(int propertyId);

    // ✅ SỬA: Trả về DTO thay vì Entity
    RoomResponseDTO addRoom(RoomRequestDTO dto, List<MultipartFile> images);

    // ✅ SỬA: Trả về DTO thay vì Entity
    RoomResponseDTO updateRoom(int roomId, RoomRequestDTO dto, List<MultipartFile> newImages);

    void deleteRoom(int roomId);
    RoomResponseDTO getRoomById(int roomId);
}