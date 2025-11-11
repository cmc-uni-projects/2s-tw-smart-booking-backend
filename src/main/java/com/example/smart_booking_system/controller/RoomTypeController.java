package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.RoomType;
import com.example.smart_booking_system.service.RoomTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roomtype")
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
                    .body("Error adding property: " + e.getMessage());
        }
    }
}
