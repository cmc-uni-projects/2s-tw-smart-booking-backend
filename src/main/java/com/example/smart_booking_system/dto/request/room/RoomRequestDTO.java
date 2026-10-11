package com.example.smart_booking_system.dto.request.room;

import com.example.smart_booking_system.enums.RoomCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomRequestDTO {

    @NotNull(message = "{validation.room.propertyRequired}")
    private Integer propertyId;

    @NotBlank(message = "{validation.room.nameRequired}")
    private String roomName;

    @NotNull(message = "{validation.room.categoryRequired}")
    private RoomCategory roomCategory; // SINGLE, DOUBLE, FAMILY, SUITE...

    @NotNull(message = "{validation.room.priceRequired}")
    @Min(value = 0, message = "{validation.room.pricePositive}")
    private BigDecimal pricePerNight;

    @Min(value = 0, message = "{validation.room.weekendPricePositive}")
    private BigDecimal weekendPrice;

    @Min(value = 1, message = "{validation.room.capacityMin}")
    private int capacity;

    private String description;

    private BigDecimal area; // Diện tích phòng (nếu có)

    // Danh sách ID của tiện nghi được chọn (vd: ["wifi", "tv", "ac"])
    private List<String> amenities;
}