package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomImageResponseDTO;
import com.example.smart_booking_system.service.RoomImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roomImage")
public class RoomImageController {

    private final RoomImageService roomImageService;

    public RoomImageController(RoomImageService roomImageService) {
        this.roomImageService = roomImageService;
    }

    @PostMapping("/upload-multiple/{roomId}")
    public ResponseEntity<?> uploadMultiple(
            @PathVariable int roomId,
            @RequestParam("file") List<MultipartFile> files) {

        List<RoomImageResponseDTO> dto =
                roomImageService.uploadMultipleRoomImages(roomId, files);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getByRoom(@PathVariable int roomId) {
        return ResponseEntity.ok(roomImageService.getImagesByRoomId(roomId));
    }

    @DeleteMapping("/delete/{roomId}/{imageId}")
    public ResponseEntity<?> delete(@PathVariable int roomId,
                                    @PathVariable int imageId) {

        roomImageService.deleteRoomImage(roomId, imageId);
        return ResponseEntity.ok("Deleted");
    }
}
