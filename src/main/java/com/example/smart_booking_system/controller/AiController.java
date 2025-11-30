package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.dto.request.ChatRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> chat(
            @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        try {
            // 1. Xác định userId (nếu chưa login → guest)
            String userId = (currentUser != null)
                    ? currentUser.getUserId()
                    : "guest-" + java.util.UUID.randomUUID().toString().substring(0, 8);

            // 2. Gọi AI service
            ChatResponseDTO aiResult = aiService.processChat(userId, request.getMessage());

            // 3. Nếu là kết quả tìm phòng → xoá câu trả lời AI
            if ("SEARCH_ROOM_RESULT".equals(aiResult.getAction())) {
                aiResult.setResponse(null);
            }

            // 4. Trả về FE
            return ResponseEntity.ok(
                    ApiResponse.success("AI trả lời thành công", aiResult)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xử lý AI: " + e.getMessage()));
        }
    }
}

