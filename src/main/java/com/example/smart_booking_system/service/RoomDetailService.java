package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.*;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.repository.RoomDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomDetailService {

    private final RoomDetailRepository roomDetailRepository;

    public RoomDetailsResponseDTO getRoomDetails(int roomId) {

        // ============================
        // 1. Room
        // ============================
        Room room = roomDetailRepository.getRoomDetail(roomId);
        if (room == null) {
            throw new RuntimeException("Room not found with id: " + roomId);
        }

        RoomResponseDTO roomDTO = new RoomResponseDTO();
        roomDTO.setRoomId(room.getRoomId());
        roomDTO.setRoomName(room.getRoomName());
        roomDTO.setRoomCategory(room.getRoomCategory());
        roomDTO.setDescription(room.getDescription());
        roomDTO.setCapacity(room.getCapacity());
        roomDTO.setPricePerNight(room.getPricePerNight());
        roomDTO.setRoomStatus(room.getRoomStatus());
        roomDTO.setActive(room.isActive());
        roomDTO.setPropertyId(room.getPropertyId().getPropertyId());


        // ============================
        // 2. Amenities
        // ============================
        List<RoomAmenity> amenities = roomDetailRepository.getAmenitiesByRoomId(roomId);

        List<RoomAmenityResponseDTO> amenityDTOs = amenities.stream().map(a -> {

            RoomAmenityResponseDTO dto = new RoomAmenityResponseDTO();
            dto.setRoomAmenityId(a.getRoomAmenityId());

            // FIX CHUẨN theo entity RoomAmenity
            dto.setRoomId(a.getRoomId().getRoomId());

            dto.setAmenityId(a.getAmenityId().getAmenityId());

            // FIX CHUẨN theo entity Amenity
            dto.setAmenityName(a.getAmenityId().getAmenityName());

            return dto;
        }).toList();


        // ============================
        // 3. Images
        // ============================
        List<RoomImage> images = roomDetailRepository.getImagesByRoomId(roomId);

        List<RoomImageResponseDTO> imageDTOs = images.stream().map(img ->
                new RoomImageResponseDTO(
                        img.getRoomImageId(),
                        img.getRoom().getRoomId(),
                        img.getImageUrl(),
                        img.isActive()
                )
        ).toList();


        // ============================
        // 4. Combine output
        // ============================
        RoomDetailsResponseDTO response = new RoomDetailsResponseDTO();
        response.setRoom(roomDTO);
        response.setAmenities(amenityDTOs);
        response.setImages(imageDTOs);

        return response;
    }
}
