package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.dto.response.ai.AiChatResponse;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.AichatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class AichatController {

    private final AichatService aiService;

    @PostMapping("/ask")
    public ResponseEntity<?> askAi(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> body
    ) {
        // 1. Lấy thông tin user
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Vui lòng đăng nhập để chat"));
        }
        String userId = userDetails.getUserId();
        String message = body.get("message");

        // 2. Gọi Service AI
        AiChatResponse response = aiService.getRecommendation(userId, message);

        // 3. Trả về kết quả cho Frontend
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.error("Hệ thống AI đang bận, vui lòng thử lại sau."));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}