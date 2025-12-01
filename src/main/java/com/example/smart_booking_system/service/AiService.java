package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.entity.AiChatHistory;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.repository.AiChatHistoryRepository;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {

    private final AiChatHistoryRepository aiChatHistoryRepository;
    private final UserRepository userRepository;
    private final PropertyDetailRepository propertyDetailRepository;
    private final ObjectMapper objectMapper;

    public AiService(AiChatHistoryRepository aiChatHistoryRepository,
                     UserRepository userRepository,
                     PropertyDetailRepository propertyDetailRepository,
                     ObjectMapper objectMapper) {

        this.aiChatHistoryRepository = aiChatHistoryRepository;
        this.userRepository = userRepository;
        this.propertyDetailRepository = propertyDetailRepository;
        this.objectMapper = objectMapper;
    }

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;



    // ============================================================
    // BASE INSTRUCTION — ĐÃ BỔ SUNG GỢI Ý DU LỊCH TRONG VIỆT NAM
    // ============================================================
    private final String BASE_INSTRUCTION = """
        You are "Travel Mate", a professional Vietnamese travel & hotel assistant.
        Always reply in Vietnamese with a polite, concise, clear tone. Tránh dài dòng.
        
        ======================================================
        GLOBAL FORMAT RULES — EXTREMELY IMPORTANT
        ======================================================
        You MUST ALWAYS follow these rules for every message:
        
        1) Absolutely NEVER use asterisk (*) in any form.
           - No "*"
           - No "**"
           - No "***"
           - No italic or bold using "*"
           - No markdown headings (#, ##, ###)
        
        2) Output MUST be plain text only.
        
        3) When presenting destinations or recommendations:
           - Each area/destination must be a title line ending with ":".
             Example:
             Đà Lạt:
           - Every detail under that title MUST start with "- " (dash + space).
             Example:
             - Thời tiết se lạnh.
             - Nhiều cảnh đẹp.
        
        4) NEVER write descriptive sentences immediately below a title without "- ".
           This is NOT allowed:
           Đà Lạt:
           Thời tiết se lạnh.
           MUST BE:
           Đà Lạt:
           - Thời tiết se lạnh.
        
        5) Keep answers short, focused, clean, and easy to scan.
        
        ======================================================
        TWO MODES — MUST NEVER MIX
        ======================================================
        
        -------------------------
        1) BOOKING MODE
        -------------------------
        Only trigger Booking Mode when user explicitly expresses booking intention:
        - đặt phòng
        - tìm phòng
        - book khách sạn
        - thuê phòng
        - đặt chỗ nghỉ
        
        You must collect EXACTLY 2 fields:
        1. city
        2. capacity
        
        Rules:
        - Nếu thiếu trường → hỏi đúng trường còn thiếu.
        - Khi đủ city + capacity → Xuất duy nhất 1 dòng lệnh:
        
        CMD_SEARCH_ROOM|city=<CITY>|capacity=<CAPACITY>
        
        STRICT RULES:
        - Không thêm chữ nào khác.
        - Không lời chào.
        - Không giải thích.
        - Không đoán thông tin.
        - Không xuất CMD_SEARCH_ROOM nếu user không muốn đặt phòng.
        
        -------------------------
        2) TRAVEL GUIDE MODE (MẶC ĐỊNH)
        -------------------------
        Kích hoạt khi user hỏi về:
        - nên đi đâu?
        - gợi ý địa điểm?
        - thời tiết/mùa đẹp?
        - du lịch Việt Nam?
        - câu hỏi không liên quan đặt phòng.
        
        Rules:
        - Chỉ tư vấn địa điểm TRONG VIỆT NAM.
        - Không hỏi city/capacity.
        - Không xuất CMD_SEARCH_ROOM trong mode này.
        - Phải dùng format chuẩn:
        
        Đà Lạt:
        - ý 1
        - ý 2
        
        Sapa:
        - ý 1
        - ý 2
        
        - Không sử dụng ký tự đặc biệt, không markdown phức tạp.
        
        ======================================================
        MODE DECISION
        ======================================================
        - User hỏi "đi đâu" → Travel Guide Mode.
        - User hỏi "đặt phòng" → Booking Mode.
        - Hỏi du lịch chung → Travel Guide Mode.
        - Nếu có cả tư vấn du lịch lẫn đặt phòng → CHỈ booking khi user nói rõ ràng.
        
        ======================================================
        HARD RESTRICTIONS
        ======================================================
        - Không bịa tình trạng phòng.
        - Không tạo địa danh không có thật.
        - Không dùng placeholder (...).
        - Chỉ xuất CMD_SEARCH_ROOM khi điều kiện chính xác 100%.
        - Giữ câu trả lời đơn giản, sạch sẽ, dễ đọc.
        """;



    @Transactional
    public ChatResponseDTO processChat(String userId, String userMessage) {

        // lưu lịch sử user
        saveHistory(userId, "user", userMessage);

        List<AiChatHistory> historyList =
                aiChatHistoryRepository.findRecentHistoryByUserId(userId);
        Collections.reverse(historyList);

        // STEP 1 → send user message to Gemini
        String aiReply = callGeminiApi(historyList, userMessage, null);
        aiReply = sanitize(aiReply);

        // ---------------- SEARCH ROOM COMMAND ----------------
        if (aiReply != null && aiReply.startsWith("CMD_SEARCH_ROOM")) {
            try {
                String city = extract(aiReply, "city");
                int capacity = Integer.parseInt(extract(aiReply, "capacity"));

                List<Property> results =
                        propertyDetailRepository.findAvailableProperties(city, capacity);

                String datasetJson =
                        buildJsonDataset(results, city, capacity);

                String replyText;
                if (results.isEmpty()) {
                    replyText = "Dạ rất tiếc, hiện tại em chưa tìm được chỗ ở phù hợp với số lượng người trong khu vực này ạ.";
                } else {
                    replyText = String.format(
                            "Dạ em đã tìm được %d chỗ ở phù hợp tại %s cho %d người ạ. Anh/chị xem danh sách gợi ý ở màn hình giúp em nhé.",
                            results.size(),
                            city,
                            capacity
                    );
                }

                saveHistory(userId, "model", replyText);
                JsonNode payload = objectMapper.readTree(datasetJson);

                return new ChatResponseDTO(
                        replyText,
                        "SEARCH_ROOM_RESULT",
                        payload
                );

            } catch (Exception e) {
                e.printStackTrace();
                String err = "Xin lỗi anh/chị, hệ thống gặp lỗi khi tìm phòng. Anh/chị thử lại giúp em nhé.";
                saveHistory(userId, "model", err);
                return new ChatResponseDTO(err, "ERROR", null);
            }
        }

        // ---------------- NORMAL CHAT (travel guide / tư vấn du lịch) ----------------
        saveHistory(userId, "model", aiReply);
        return new ChatResponseDTO(aiReply, "NORMAL_CHAT", null);
    }


    private String buildJsonDataset(
            List<Property> properties,
            String city,
            int capacity
    ) {

        ObjectNode root = objectMapper.createObjectNode();

        root.put("city", city);
        root.put("capacity", capacity);

        if (properties.isEmpty()) {
            root.put("type", "NO_RESULT");
            return root.toString();
        }

        root.put("type", "RESULT");
        ArrayNode list = root.putArray("properties");

        for (Property p : properties) {

            ObjectNode prop = list.addObject();

            prop.put("propertyId", p.getPropertyId());
            prop.put("name", p.getPropertyName());
            prop.put("rating", p.getRating().doubleValue());
            prop.put("reviewCount", p.getReviewCount());
            prop.put("address", p.getAddress());

            String bookingUrl = frontendUrl + "/hotels/" + p.getPropertyId();
            prop.put("bookingUrl", bookingUrl);

            ArrayNode roomArr = prop.putArray("rooms");

            for (Room r : p.getRooms()) {
                if (r.isActive() && r.getCapacity() >= capacity) {
                    ObjectNode room = roomArr.addObject();
                    room.put("roomName", r.getRoomName());
                    room.put("capacity", r.getCapacity());
                    room.put("price", r.getPricePerNight().longValue());
                }
            }
        }

        return root.toString();
    }


    private String callGeminiApi(List<AiChatHistory> history, String newMessage, String roleOverride) {

        WebClient webClient = WebClient.builder().baseUrl(apiUrl).build();
        ObjectNode requestBody = objectMapper.createObjectNode();

        // luôn cập nhật ngày
        String currentDate = LocalDateTime.now().toString();
        String finalInstruction = BASE_INSTRUCTION;

        // SYSTEM INSTRUCTION
        ObjectNode sys = objectMapper.createObjectNode();
        sys.putArray("parts").addObject().put(
                "text",
                finalInstruction
                        + "\n\nEXTRA RULES:"
                        + "\n- Today: " + currentDate
                        + "\n- Always answer in Vietnamese."
                        + "\n- Never invent data."
                        + "\n- Only output CMD_SEARCH_ROOM when user clearly wants to book."
        );
        requestBody.set("system_instruction", sys);

        // HISTORY
        ArrayNode contents = requestBody.putArray("contents");
        for (AiChatHistory h : history) {
            ObjectNode node = contents.addObject();
            node.put("role", h.getSenderRole());
            node.putArray("parts").addObject().put("text", h.getMessageContent());
        }

        // NEW MESSAGE
        ObjectNode msg = contents.addObject();
        msg.put("role", roleOverride != null ? roleOverride : "user");
        msg.putArray("parts").addObject().put("text", newMessage);

        try {
            String raw = webClient.post()
                    .uri(uri -> uri.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofMillis(400))
                                    .filter(ex -> ex instanceof WebClientResponseException.ServiceUnavailable)
                    )
                    .block();

            if (raw == null || raw.isEmpty()) {
                return "Xin lỗi anh/chị, hệ thống đang bận ạ.";
            }

            JsonNode root = objectMapper.readTree(raw);

            if (!root.has("candidates"))
                return "Xin lỗi anh/chị, em chưa nhận được phản hồi từ hệ thống.";

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0)
                return "Xin lỗi anh/chị, hệ thống đang bận.";

            JsonNode candidate = candidates.get(0);
            if (candidate == null || !candidate.has("content"))
                return "Xin lỗi anh/chị, hệ thống chưa trả lời.";

            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray() || parts.size() == 0)
                return "Xin lỗi anh/chị, em chưa nhận được nội dung trả lời.";

            String aiRawReply = parts.get(0).path("text").asText("");

            return aiRawReply;

        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hệ thống đang quá tải. Anh/chị thử lại giúp em ạ.";
        }
    }


    private String extract(String src, String key) {
        Matcher m = Pattern.compile(key + "=(.*?)(\\||$)").matcher(src);
        return m.find() ? m.group(1).trim() : "";
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replace("```", "")
                .replace("---", "")
                .trim();
    }

    private void saveHistory(String userId, String role, String content) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        AiChatHistory history = AiChatHistory.builder()
                .userId(user)
                .senderRole(role)
                .messageContent(content)
                .timestamp(LocalDateTime.now())
                .build();

        aiChatHistoryRepository.save(history);
    }

}
