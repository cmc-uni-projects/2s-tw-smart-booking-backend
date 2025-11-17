package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.ai.AiChatRequest;
import com.example.smart_booking_system.dto.response.ai.AiChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AichatService {

    // Địa chỉ của Python Service (đang chạy port 8386)
    private final String PYTHON_API_URL = "http://localhost:8080/api/recommend";

    public AiChatResponse getRecommendation(String userId, String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. Tạo dữ liệu gửi đi
        AiChatRequest request = new AiChatRequest(userId, userMessage);

        try {
            // 2. Gọi POST sang Python và nhận kết quả
            AiChatResponse response = restTemplate.postForObject(
                    PYTHON_API_URL,
                    request,
                    AiChatResponse.class
            );
            return response;
        } catch (Exception e) {
            // Xử lý nếu Python sập hoặc lỗi
            System.err.println("❌ Lỗi gọi AI Service: " + e.getMessage());
            return null; // Hoặc throw exception tùy bạn
        }
    }
}