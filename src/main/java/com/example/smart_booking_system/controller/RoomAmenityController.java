package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.service.RoomAmenityService;
import com.example.smart_booking_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roomAmenity")
@RequiredArgsConstructor
public class RoomAmenityController {

    private final RoomAmenityService roomAmenityService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> addAmenity(
            @RequestParam int roomId,
            @RequestParam int amenityId
    ) {
        try {
            RoomAmenity ra = roomAmenityService.addRoomAmenity(roomId, amenityId);
            return ResponseEntity.status(201).body(roomAmenityService.toDTO(ra));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


}