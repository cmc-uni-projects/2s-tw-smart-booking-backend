package com.example.smart_booking_system.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PropertySimpleDTO {

    private int propertyId;
    private String propertyName;
    private BigDecimal rating;
    private int reviewCount;
    private String city;
    private String propertyType;
}
