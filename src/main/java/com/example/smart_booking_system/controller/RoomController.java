package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
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


}
