package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomAmenityDTO;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.repository.AmenityRepository;
import com.example.smart_booking_system.repository.RoomAmenityRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomAmenityService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    public RoomAmenity addRoomAmenity(int roomId, int amenityId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found"));

        RoomAmenity ra = new RoomAmenity();
        ra.setRoomId(room);
        ra.setAmenityId(amenity);
        ra.setActive(true);

        return roomAmenityRepository.save(ra);
    }

    public RoomAmenityDTO toDTO(RoomAmenity ra) {
        return RoomAmenityDTO.builder()
                .roomAmenityId(ra.getRoomAmenityId())
                .roomId(ra.getRoomId().getRoomId())
                .amenityId(ra.getAmenityId().getAmenityId())
                .amenityName(ra.getAmenityId().getAmenityName())
                .build();
    }

}