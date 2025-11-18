package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.admin.PropertyReviewDTO;
import com.example.smart_booking_system.dto.request.property.PropertyApplicationSubmitDTO;
import com.example.smart_booking_system.dto.response.property.PropertyDetailDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.enums.PropertyStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PropertyService {

    Property addProperty(Property property, String ownerId);

    List<Property> searchProperties(String keyword, Integer guests);

    PropertyDetailDTO updateProperty(int id, Property updatedProperty);

    List<PropertyDetailDTO> getFeaturedProperties();

    List<PropertyDetailDTO> getPropertiesByStatus(PropertyStatus status);

    PropertyDetailDTO reviewProperty(Integer propertyId, PropertyReviewDTO reviewDTO, String adminUsername);

    PropertyDetailDTO submitPropertyApplication(PropertyApplicationSubmitDTO dto, List<MultipartFile> images, String ownerId);
    List<PropertyDetailDTO> getOwnerProperties(String ownerId);

    List<PropertyDetailDTO> getOwnerActiveProperties(String ownerId);
}