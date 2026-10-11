package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.dto.request.room.RoomRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.PriceForecastDTO;
import com.example.smart_booking_system.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final Messages messages;

    private final RoomService roomService;

    // 1. Lấy danh sách phòng
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getRoomsByProperty(@PathVariable int propertyId) {
        List<RoomResponseDTO> rooms = roomService.getRoomsByPropertyId(propertyId);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.room.listed"), rooms));
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
            return ResponseEntity.ok(ApiResponse.success(messages.t("api.room.created"), savedRoom));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messages.of(e)));
        }
    }

    // 3. Xóa phòng
    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteRoom(@PathVariable int roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.room.deleted"), null));
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

            return ResponseEntity.ok(ApiResponse.success(messages.t("api.updated"), updatedRoom));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messages.of(e)));
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomDetail(@PathVariable int roomId) {
        RoomResponseDTO room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.room.loaded"), room));
    }

    // 5. Kiểm tra tên phòng đã tồn tại trong cùng cơ sở lưu trú chưa
    @GetMapping("/check-name")
    public ResponseEntity<?> checkRoomName(
            @RequestParam int propertyId,
            @RequestParam String roomName,
            @RequestParam(defaultValue = "0") int excludeRoomId // Mặc định 0 nếu thêm mới
    ) {
        boolean exists = roomService.checkRoomNameExists(propertyId, roomName, excludeRoomId);
        if (exists) {
            return ResponseEntity.status(409).body(ApiResponse.error(messages.t("error.room.nameTaken")));
        }
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.name.available"), true));
    }

    // Dự báo giá
    @GetMapping("/{roomId}/price-forecast")
    public ResponseEntity<?> getPriceForecast(
            @PathVariable int roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "14") int days
    ) {
        List<PriceForecastDTO> forecast = roomService.getPriceForecast(roomId, startDate, days);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.room.forecast"), forecast));
    }

}