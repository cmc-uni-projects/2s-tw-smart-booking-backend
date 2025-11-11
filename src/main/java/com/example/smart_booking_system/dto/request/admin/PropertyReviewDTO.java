package com.example.smart_booking_system.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PropertyReviewDTO {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "APPROVE|REJECTED", message = "Trạng thái phải là APPROVE hoặc REJECTED")
    private String status;

    private String reason;
}