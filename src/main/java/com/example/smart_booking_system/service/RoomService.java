package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomResponseDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PropertyRepository propertyRepository;

    public RoomService(RoomRepository roomRepository, PropertyRepository propertyRepository) {
        this.roomRepository = roomRepository;
        this.propertyRepository = propertyRepository;
    }

    public Room addRoom(Room room) {

        if (room.getPropertyId() == null || room.getPropertyId().getPropertyId() <= 0) {
            throw new IllegalArgumentException("Property ID cannot be null or invalid.");
        }

        Property property = propertyRepository.findById(room.getPropertyId().getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found with id: " + room.getPropertyId().getPropertyId()));

        if (room.getRoomName() == null || room.getRoomName().trim().isEmpty()) {
            throw new IllegalArgumentException("Room name cannot be null or empty.");
        }

        if (room.getDescription() == null || room.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty.");
        }

        if (room.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be greater than 0.");
        }

        if (room.getPricePerNight() == null || room.getPricePerNight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per night must be greater than 0.");
        }

        if (room.getRoomCategory() == null) {
            throw new IllegalArgumentException("Room category cannot be null.");
        }

        boolean validCategory = Arrays.stream(RoomCategory.values())
                .anyMatch(c -> c.equals(room.getRoomCategory()));
        if (!validCategory) {
            throw new IllegalArgumentException("Invalid room category: " + room.getRoomCategory());
        }

        if (room.getRoomStatus() == null) {
            throw new IllegalArgumentException("Room status cannot be null.");
        }

        boolean validStatus = Arrays.stream(RoomStatus.values())
                .anyMatch(s -> s.equals(room.getRoomStatus()));
        if (!validStatus) {
            throw new IllegalArgumentException("Invalid room status: " + room.getRoomStatus());
        }

        room.setPropertyId(property);
        room.setActive(true);

        return roomRepository.save(room);
    }

    private RoomResponseDTO toDTO(Room room) {
        return RoomResponseDTO.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomCategory(room.getRoomCategory())
                .description(room.getDescription())
                .capacity(room.getCapacity())
                .pricePerNight(room.getPricePerNight())
                .roomStatus(room.getRoomStatus())
                .isActive(room.isActive())
                .propertyId(room.getPropertyId().getPropertyId())
                .build();
    }

    public List<RoomResponseDTO> searchRooms(Integer propertyId, String keyword) {
        List<Room> rooms = roomRepository.searchRooms(propertyId, keyword);

        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("No rooms found matching your search criteria.");
        }

        return rooms.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public RoomResponseDTO updateRoom(int id, Room updatedRoom) {
        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));

        if (updatedRoom.getRoomName() != null && !updatedRoom.getRoomName().trim().isEmpty()) {
            existingRoom.setRoomName(updatedRoom.getRoomName());
        }

        if (updatedRoom.getDescription() != null && !updatedRoom.getDescription().trim().isEmpty()) {
            existingRoom.setDescription(updatedRoom.getDescription());
        }

        if (updatedRoom.getCapacity() > 0) {
            existingRoom.setCapacity(updatedRoom.getCapacity());
        }

        if (updatedRoom.getPricePerNight() != null && updatedRoom.getPricePerNight().compareTo(BigDecimal.ZERO) > 0) {
            existingRoom.setPricePerNight(updatedRoom.getPricePerNight());
        }

        if (updatedRoom.getRoomCategory() != null) {
            boolean validCategory = Arrays.stream(RoomCategory.values())
                    .anyMatch(c -> c.equals(updatedRoom.getRoomCategory()));
            if (!validCategory) {
                throw new IllegalArgumentException("Invalid room category: " + updatedRoom.getRoomCategory());
            }
            existingRoom.setRoomCategory(updatedRoom.getRoomCategory());
        }

        if (updatedRoom.getRoomStatus() != null) {
            boolean validStatus = Arrays.stream(RoomStatus.values())
                    .anyMatch(s -> s.equals(updatedRoom.getRoomStatus()));
            if (!validStatus) {
                throw new IllegalArgumentException("Invalid room status: " + updatedRoom.getRoomStatus());
            }
            existingRoom.setRoomStatus(updatedRoom.getRoomStatus());
        }

        existingRoom.setActive(updatedRoom.isActive());
        existingRoom.setPropertyId(existingRoom.getPropertyId());

        Room savedRoom = roomRepository.save(existingRoom);

        return toDTO(savedRoom);
    }

}
