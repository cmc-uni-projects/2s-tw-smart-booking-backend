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


@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    final private AiService aiService;
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> chat(
            @RequestBody ChatRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập để sử dụng trợ lý AI."));
        }

        try {
            // 2. Gọi Service xử lý
            String userId = currentUser.getUserId();
            ChatResponseDTO response = aiService.processChat(userId, request.getMessage());

            // 3. Trả về kết quả
            return ResponseEntity.ok(ApiResponse.success("AI trả lời thành công", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi xử lý AI: " + e.getMessage()));
        }
    }
}
