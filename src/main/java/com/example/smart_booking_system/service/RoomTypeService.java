package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.RoomTypeResponse;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.RoomType;
import com.example.smart_booking_system.mapper.RoomTypeMapper;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final PropertyRepository propertyRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository,
                           PropertyRepository propertyRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.propertyRepository = propertyRepository;
    }

    public RoomType addRoomType(RoomType roomType) {
        if (roomType.getRoomTypeName() == null || roomType.getRoomTypeName().trim().isEmpty()) {
            throw new IllegalArgumentException("Room type name cannot be empty");
        }

        if (roomType.getDescription() == null || roomType.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("Room type description cannot be empty");
        }

        if (roomType.getCapacity() < 0) {
            throw new IllegalArgumentException("Room type capacity cannot be less than 0");
        }

        if (roomType.getPolicy() == null || roomType.getPolicy().trim().isEmpty()) {
            throw new IllegalArgumentException("Room type policy cannot be empty");
        }

        if (roomType.getPropertyId() == null || roomType.getPropertyId().getPropertyId() <= 0) {
            throw new IllegalArgumentException("Room type must be linked with a valid property");
        }

        int propertyId = roomType.getPropertyId().getPropertyId();
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found with id: " + propertyId));

        roomType.setPropertyId(property);
        return roomTypeRepository.save(roomType);
    }

    public List<RoomTypeResponse> getRoomTypesByProperty(int propertyId) {
        if (propertyId <= 0) {
            throw new IllegalArgumentException("Property ID must be greater than 0");
        }

        List<RoomType> roomTypes = roomTypeRepository.findRoomTypesByPropertyId(propertyId);

        if (roomTypes.isEmpty()) {
            throw new IllegalArgumentException("No room types found for property ID: " + propertyId);
        }

        return roomTypes.stream()
                .map(RoomTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RoomTypeResponse getRoomTypeById(int roomTypeId) {
        if (roomTypeId <= 0) {
            throw new IllegalArgumentException("Room type ID must be greater than 0");
        }

        RoomType roomType = roomTypeRepository.findRoomTypeById(roomTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Room type not found with id: " + roomTypeId));

        return RoomTypeMapper.toResponse(roomType);
    }
}
