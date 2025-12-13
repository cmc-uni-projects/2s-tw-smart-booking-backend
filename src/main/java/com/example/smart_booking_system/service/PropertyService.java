package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.dto.response.property.PropertyMapDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface PropertyService {

    Property addProperty(Property property, String ownerId);

    PropertyDetailDTO updateProperty(int id, Property updatedProperty);

    List<PropertyDetailDTO> getFeaturedProperties();

    List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status);

    PropertyDetailDTO reviewProperty(Integer propertyId, PropertyReviewDTO reviewDTO, String adminUsername);

    PropertyDetailDTO getPropertyDetailById(Integer id, LocalDate checkIn, LocalDate checkOut);

    PropertyDetailDTO submitPropertyApplication(PropertyApplicationSubmitDTO dto,
                                                List<MultipartFile> images,
                                                String ownerId);

    List<PropertyDetailDTO> getOwnerProperties(String ownerId);

    List<PropertyDetailDTO> getOwnerActiveProperties(String ownerId);

    List<PropertyMapDTO> findNearbyProperties(Double lat, Double lng, Double radius);

    List<PropertyDetailDTO> searchProperties(String keyword,
                                             Integer guests,
                                             LocalDate checkIn,
                                             LocalDate checkOut);

    boolean checkNameAvailability(String propertyName);

    boolean togglePropertyStatus(Integer propertyId, String ownerId);
}
