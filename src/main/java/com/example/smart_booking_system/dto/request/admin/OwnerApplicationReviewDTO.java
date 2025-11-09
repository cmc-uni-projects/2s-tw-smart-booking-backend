package com.example.smart_booking_system.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OwnerApplicationReviewDTO {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "APPROVED|REJECTED", message = "Trạng thái phải là APPROVED hoặc REJECTED")
    private String status;

    private String reason;
}