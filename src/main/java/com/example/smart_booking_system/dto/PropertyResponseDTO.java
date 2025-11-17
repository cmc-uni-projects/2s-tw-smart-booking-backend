package com.example.smart_booking_system.dto;

import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PropertyResponseDTO {

    private int propertyId;
    private String propertyName;
    private PropertyType propertyType;

    private String address;
    private String country;

    // ✅ THÊM MỚI: Tỉnh/Thành phố
    private String province;

    private String city;
    private String postalCode;

    private String description;

    // ✅ THÊM MỚI: Diện tích (Lấy từ bảng PropertyDetail)
    private BigDecimal area;

    // ❌ ĐÃ XÓA: amenitiesJson, imageUrlsJson
    // (Dữ liệu này sẽ được trả về qua các list riêng trong PropertyDetailsResponseDTO)

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String phoneContact;
    private String emailContact;

    private BigDecimal rating;
    private int reviewCount;

    private boolean isActive;
    private PropertyStatus propertyStatus;

    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String coverImage;
}