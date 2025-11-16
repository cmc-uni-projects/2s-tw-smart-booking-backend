package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/room")
public class RoomController {
    private final RoomService roomService;

    public RoomController(RoomService roomService){
        this.roomService=roomService;
    }
    @PostMapping("/add")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> addRoom(@RequestBody Room room) {
        try {
            Room savedRoom = roomService.addRoom(room);

            return ResponseEntity
                    .status(201)
                    .body(savedRoom);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error adding room: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> searchRooms(
            @RequestParam(required = false) Integer propertyId,
            @RequestParam(required = false) String keyword
    ) {
        try {
            return ResponseEntity.ok(roomService.searchRooms(propertyId, keyword));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error searching rooms: " + e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateRoom(
            @PathVariable int id,
            @RequestBody Room updatedRoom
    ) {
        try {
            RoomResponseDTO updated = roomService.updateRoom(id, updatedRoom);

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error updating room: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoom(@PathVariable int id) {
        try {
            String message = roomService.deleteRoom(id);

            return ResponseEntity.ok().body(message);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error deleting room: " + e.getMessage());
        }
    }

    @PutMapping("/set-active/{id}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> setRoomActiveStatus(
            @PathVariable int id,
            @RequestParam boolean isActive
    ) {
        try {
            String message = roomService.updateRoomActiveStatus(id, isActive);
            return ResponseEntity.ok(message);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body("Error updating room active status: " + e.getMessage());
        }
    }


}
