package com.example.smart_booking_system.dto.request.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequest {
    private String user_id; // Phải trùng tên field bên Python (snake_case)
    private String message;
}