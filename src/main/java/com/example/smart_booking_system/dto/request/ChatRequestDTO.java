package com.example.smart_booking_system.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequestDTO {
    @NotBlank(message = "{validation.chat.messageRequired}")
    private String message;
}
