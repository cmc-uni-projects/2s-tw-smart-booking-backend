package com.example.smart_booking_system.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendRequestDTO {
    @NotBlank(message = "{validation.reason.required}")
    private String reason;
}