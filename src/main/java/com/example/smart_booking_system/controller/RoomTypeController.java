package com.example.smart_booking_system.controller;


import com.example.smart_booking_system.dto.RoomTypeResponse;
import com.example.smart_booking_system.entity.RoomType;
import com.example.smart_booking_system.service.RoomTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/roomtype")
public class RoomTypeController {
    private final RoomTypeService roomTypeService;
    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?>  addRoomType(@RequestBody RoomType roomType)
    {
        try {
            RoomType savedRoomType = roomTypeService.addRoomType(roomType);
            return ResponseEntity
                    .status(201)
                    .body(savedRoomType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error adding room types: " + e.getMessage());
        }
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<RoomTypeResponse>> getRoomTypesByProperty(@PathVariable int propertyId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypesByProperty(propertyId));
    }


    @GetMapping("/{roomTypeId}")
    public ResponseEntity<RoomTypeResponse> getRoomTypeById(@PathVariable int roomTypeId) {
        return ResponseEntity.ok(roomTypeService.getRoomTypeById(roomTypeId));
    }

    @PutMapping("/update/{roomTypeId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateRoomType(@PathVariable int roomTypeId,
                                            @RequestBody RoomType updatedRoomType) {
        try {
            RoomTypeResponse updated = roomTypeService.updateRoomType(roomTypeId, updatedRoomType);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating room type: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{roomTypeId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoomType(@PathVariable int roomTypeId) {
        try {
            roomTypeService.deleteRoomType(roomTypeId);
            return ResponseEntity.ok("Room type deactivated successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting room type: " + e.getMessage());
        }
    }


}
