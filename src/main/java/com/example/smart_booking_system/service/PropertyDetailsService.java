package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.*;
import com.example.smart_booking_system.entity.*;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyDetailsService {

    private final PropertyDetailRepository propertyDetailRepository;

    public PropertyDetailsResponseDTO getPropertyDetails(int propertyId) {

        // ============================
        // 1. Lấy Property
        // ============================
        Property property = propertyDetailRepository.getPropertyDetail(propertyId);
        if (property == null) {
            throw new RuntimeException("Property not found with id: " + propertyId);
        }

        // Map Property → PropertyResponseDTO
        PropertyResponseDTO propertyDTO = new PropertyResponseDTO();
        propertyDTO.setPropertyId(property.getPropertyId());
        propertyDTO.setPropertyName(property.getPropertyName());
        propertyDTO.setPropertyType(property.getPropertyType());
        propertyDTO.setAddress(property.getAddress());
        propertyDTO.setCountry(property.getCountry());
        propertyDTO.setCity(property.getCity());
        propertyDTO.setPostalCode(property.getPostalCode());
        propertyDTO.setDescription(property.getDescription());
        propertyDTO.setAmenitiesJson(property.getAmenitiesJson());
        propertyDTO.setImageUrlsJson(property.getImageUrlsJson());
        propertyDTO.setLatitude(property.getLatitude());
        propertyDTO.setLongitude(property.getLongitude());
        propertyDTO.setPhoneContact(property.getPhoneContact());
        propertyDTO.setEmailContact(property.getEmailContact());
        propertyDTO.setRating(property.getRating());
        propertyDTO.setReviewCount(property.getReviewCount());
        propertyDTO.setActive(property.isActive());
        propertyDTO.setPropertyStatus(property.getPropertyStatus());
        propertyDTO.setCreatedAt(property.getCreatedAt());
        propertyDTO.setUpdatedAt(property.getUpdatedAt());



        // ============================
        // 2. Lấy Rooms theo propertyId
        // ============================
        List<Room> rooms = propertyDetailRepository.getRoomsByPropertyId(propertyId);

        List<RoomResponseDTO> roomDTOs = rooms.stream().map(room -> {
            RoomResponseDTO dto = new RoomResponseDTO();
            dto.setRoomId(room.getRoomId());
            dto.setRoomName(room.getRoomName());
            dto.setRoomCategory(room.getRoomCategory());
            dto.setDescription(room.getDescription());
            dto.setCapacity(room.getCapacity());
            dto.setPricePerNight(room.getPricePerNight());
            dto.setRoomStatus(room.getRoomStatus());
            dto.setActive(room.isActive());
            dto.setPropertyId(room.getPropertyId().getPropertyId());
            return dto;
        }).toList();


        // ============================
        // 3. Lấy Amenities
        // ============================
        List<PropertyAmenity> amenities = propertyDetailRepository.getAmenitiesByPropertyId(propertyId);

        List<PropertyAmenityResponseDTO> amenityDTOs = amenities.stream().map(a -> {
            PropertyAmenityResponseDTO dto = new PropertyAmenityResponseDTO();
            dto.setPropertyAmenityId(a.getPropertyAmenityId());
            dto.setPropertyId(a.getProperty().getPropertyId());
            dto.setAmenityId(a.getAmenity().getAmenityId());
            dto.setAmenityName(a.getAmenity().getAmenityName());
            return dto;
        }).toList();


        // ============================
        // 4. Lấy Images
        // ============================
        List<PropertyImage> images = propertyDetailRepository.getImagesByPropertyId(propertyId);

        List<PropertyImageResponseDTO> imageDTOs = images.stream().map(img ->
                new PropertyImageResponseDTO(
                        img.getPropertyImageId(),
                        img.getProperty().getPropertyId(),
                        img.getImageUrl(),
                        img.isActive()
                )
        ).toList();


        // ============================
        // 5. Gộp tất cả vào DTO tổng
        // ============================
        PropertyDetailsResponseDTO result = new PropertyDetailsResponseDTO();
        result.setProperty(propertyDTO);
        result.setRooms(roomDTOs);
        result.setAmenities(amenityDTOs);
        result.setImages(imageDTOs);

        return result;
    }
}
