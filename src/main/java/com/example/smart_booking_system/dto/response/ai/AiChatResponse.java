package com.example.smart_booking_system.dto.response.ai;

import lombok.Data;
import java.util.List;

@Data
public class AiChatResponse {
    private String user_history;
    private List<RecommendationItem> recommendations;

    @Data
    public static class RecommendationItem {
        private int property_id;
        private String info;
    }
}