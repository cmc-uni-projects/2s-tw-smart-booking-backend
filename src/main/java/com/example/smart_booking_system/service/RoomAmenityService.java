package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomAmenityDTO;
import com.example.smart_booking_system.entity.Amenity;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.entity.RoomAmenity;
import com.example.smart_booking_system.enums.AmenityType;
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

        // CHECK 1: amenity phải là ROOM
        if (amenity.getAmenityType() != AmenityType.ROOM) {
            throw new IllegalArgumentException("This amenity is not a ROOM amenity");
        }

        // CHECK 2: tránh thêm trùng
        if (roomAmenityRepository.existsActive(roomId, amenityId)) {
            throw new IllegalArgumentException("Amenity already added to this room");
        }

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

    public List<RoomAmenityDTO> getAmenitiesByRoomId(int roomId) {
        List<RoomAmenity> list = roomAmenityRepository.findActiveByRoomId(roomId);

        return list.stream()
                .map(this::toDTO)
                .toList();
    }

    public RoomAmenityDTO updateRoomAmenity(int roomAmenityId, int newAmenityId) {

        RoomAmenity ra = roomAmenityRepository.findById(roomAmenityId)
                .orElseThrow(() -> new IllegalArgumentException("RoomAmenity not found"));

        // Check amenity exists
        Amenity amenity = amenityRepository.findById(newAmenityId)
                .orElseThrow(() -> new IllegalArgumentException("Amenity not found"));

        // Check trùng roomId + newAmenityId
        int roomId = ra.getRoomId().getRoomId();

        if (roomAmenityRepository.existsActiveExcept(roomId, newAmenityId, roomAmenityId)) {
            throw new IllegalArgumentException("This amenity already exists in this room");
        }

        // Update
        ra.setAmenityId(amenity);

        RoomAmenity saved = roomAmenityRepository.save(ra);

        return toDTO(saved);
    }

    public String deleteRoomAmenity(int id) {

        RoomAmenity ra = roomAmenityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RoomAmenity not found"));

        ra.setActive(false);
        roomAmenityRepository.save(ra);

        return "RoomAmenity deleted (soft delete) successfully.";
    }




}