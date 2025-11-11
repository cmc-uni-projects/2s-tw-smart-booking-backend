package com.example.smart_booking_system.dto.response.property;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FeaturedPropertyDTO {
    private Integer propertyId;
    private String propertyName;
    private String city;
    private BigDecimal rating;
    private int reviewCount;

}