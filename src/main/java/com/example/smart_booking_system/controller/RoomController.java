package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 1. Lấy danh sách phòng
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getRoomsByProperty(@PathVariable int propertyId) {
        List<RoomResponseDTO> rooms = roomService.getRoomsByPropertyId(propertyId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phòng thành công", rooms));
    }

    // 2. Thêm phòng mới (Multipart)
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> addRoom(
            @RequestPart("roomData") String roomDataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            RoomRequestDTO dto = mapper.readValue(roomDataJson, RoomRequestDTO.class);

            RoomResponseDTO savedRoom = roomService.addRoom(dto, images);
            return ResponseEntity.ok(ApiResponse.success("Thêm phòng thành công", savedRoom));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi thêm phòng: " + e.getMessage()));
        }
    }

    // 3. Xóa phòng
    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteRoom(@PathVariable int roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Xóa phòng thành công", null));
    }

    // 4. Cập nhật (Hỗ trợ cả thông tin & upload thêm ảnh)
    // ✅ SỬA LẠI: Dùng Multipart giống hệt Add để nhận ảnh
    @PutMapping(value = "/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateRoom(
            @PathVariable int roomId,
            @RequestPart("roomData") String roomDataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        try {
            // Parse JSON thủ công
            ObjectMapper mapper = new ObjectMapper();
            RoomRequestDTO dto = mapper.readValue(roomDataJson, RoomRequestDTO.class);

            // ✅ Gọi Service truyền cả DTO và Images
            RoomResponseDTO updatedRoom = roomService.updateRoom(roomId, dto, images);

            return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", updatedRoom));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi cập nhật: " + e.getMessage()));
        }
    }
}