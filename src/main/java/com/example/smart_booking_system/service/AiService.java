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
import java.time.LocalDate;
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

    // ============================
    // BASE INSTRUCTION (ENGLISH)
    // ============================
    private final String BASE_INSTRUCTION = """
You are "Travel Mate", a professional Vietnamese hotel booking assistant.
You must ALWAYS reply in Vietnamese with a friendly, polite tone.

======================================================
REQUIRED SLOTS (MUST COLLECT ALL 4)
======================================================
To perform a room search, you MUST collect ALL FOUR fields:
1. city
2. capacity
3. checkIn (YYYY-MM-DD)
4. checkOut (YYYY-MM-DD)

RULES:
- If ANY slot is missing or unclear, politely ask the user.
- If ALL FOUR slots are present → OUTPUT the search command immediately.
- Do NOT ask the user to confirm again.

======================================================
DATE INTERPRETATION RULES
======================================================
Current system datetime: [CURRENT_DATE].
Convert phrases like:
- "ngày mai"
- "cuối tuần này"
- "20/10"
to actual YYYY-MM-DD dates.
If unclear → ask again.

======================================================
SEARCH COMMAND RULE
======================================================
When all 4 required fields exist, output EXACTLY one line:

CMD_SEARCH_ROOM|city=<CITY>|capacity=<CAPACITY>|checkIn=<YYYY-MM-DD>|checkOut=<YYYY-MM-DD>

IMPORTANT:
- The command must appear alone in the message.
- Do NOT add explanation in the same message.
- Do NOT ask the user to confirm.
- Backend will handle the search.

======================================================
HARD RESTRICTIONS
======================================================
- Never invent details.
- Never assume the user saw earlier messages.
- Never output template examples unless using REAL values.
""";


    // ============================================================
    // MAIN CHAT PROCESSOR
    // ============================================================
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

                LocalDate checkIn = LocalDate.parse(extract(aiReply, "checkIn"));
                LocalDate checkOut = LocalDate.parse(extract(aiReply, "checkOut"));

                // Query DB bằng PropertyDetailRepository (thay cho AiRepository)
                List<Property> results =
                        propertyDetailRepository.findAvailableProperties(city, capacity, checkIn, checkOut);

                // Build JSON dataset để FE dùng render UI đẹp
                String datasetJson =
                        buildJsonDataset(results, city, capacity, checkIn, checkOut);

                // Text trả về cho user trong khung chat (không nhờ AI nữa)
                String replyText;
                if (results.isEmpty()) {
                    replyText = "Dạ rất tiếc, hiện tại em chưa tìm được chỗ ở phù hợp với yêu cầu của anh/chị trong khoảng thời gian đó ạ.";
                } else {
                    replyText = String.format(
                            "Dạ em đã tìm được %d chỗ ở phù hợp tại %s cho %d người, từ %s đến %s ạ. Anh/chị xem danh sách gợi ý ở màn hình giúp em nhé.",
                            results.size(),
                            city,
                            capacity,
                            checkIn,
                            checkOut
                    );
                }

                // Lưu lịch sử AI (chỉ lưu text, không lưu dataset)
                saveHistory(userId, "model", replyText);

                // payload gửi cho FE (dạng JsonNode để FE xài dễ hơn)
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

        // ---------------- NORMAL CHAT ----------------
        saveHistory(userId, "model", aiReply);
        return new ChatResponseDTO(aiReply, "NORMAL_CHAT", null);
    }


    // ============================================================
    // JSON DATASET BUILDER (Anti-hallucination) - cho FE
    // ============================================================
    private String buildJsonDataset(
            List<Property> properties,
            String city,
            int capacity,
            LocalDate in,
            LocalDate out
    ) {

        ObjectNode root = objectMapper.createObjectNode();

        root.put("city", city);
        root.put("capacity", capacity);
        root.put("checkIn", in.toString());
        root.put("checkOut", out.toString());

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

            ArrayNode roomArr = prop.putArray("rooms");
            for (Room r : p.getRooms()) {

                if (r.isActive() && r.getCapacity() >= capacity) {
                    ObjectNode room = roomArr.addObject();
                    room.put("roomName", r.getRoomName());
                    room.put("capacity", r.getCapacity());
                    room.put("price", r.getPricePerNight().longValue());

                    String bookingUrl = frontendUrl + "/booking/" + r.getRoomId();
                    room.put("bookingUrl", bookingUrl);
                }
            }
        }

        return root.toString();
    }

    // ============================================================
    // CALL GEMINI API — with CURRENT_DATE, retry, timeout
    // ============================================================
    private String callGeminiApi(List<AiChatHistory> history, String newMessage, String roleOverride) {

        WebClient webClient = WebClient.builder().baseUrl(apiUrl).build();
        ObjectNode requestBody = objectMapper.createObjectNode();

        // luôn cập nhật ngày hiện tại
        String currentDate = LocalDateTime.now().toString();
        String finalInstruction = BASE_INSTRUCTION.replace("[CURRENT_DATE]", currentDate);

        // SYSTEM INSTRUCTION
        ObjectNode sys = objectMapper.createObjectNode();
        sys.putArray("parts").addObject().put(
                "text",
                finalInstruction
                        + "\n\nEXTRA RULES:"
                        + "\n- Today: " + currentDate
                        + "\n- Always answer in Vietnamese."
                        + "\n- Never invent data."
                        + "\n- Only output CMD_SEARCH_ROOM when all required info is confirmed."
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

            // SAFETY CHECK 1 — does "candidates" exist?
            if (!root.has("candidates")) {
                return "Xin lỗi anh/chị, em chưa nhận được phản hồi từ hệ thống. Anh/chị thử lại giúp em nhé.";
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0) {
                return "Xin lỗi, hệ thống đang bận. Anh/chị thử lại giúp em ạ.";
            }

            // SAFETY CHECK 2 — first candidate
            JsonNode candidate = candidates.get(0);
            if (candidate == null || !candidate.has("content")) {
                return "Xin lỗi anh/chị, hệ thống chưa trả lời. Anh/chị thử lại giúp em nhé.";
            }

            // SAFETY CHECK 3 — parts
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray() || parts.size() == 0) {
                return "Xin lỗi anh/chị, em chưa nhận được nội dung trả lời. Anh/chị thử lại giúp em nhé.";
            }

            String aiRawReply = parts.get(0).path("text").asText("");

            // HARD FILTER — ngăn Gemini lộ template lệnh mẫu
            if (aiRawReply.contains("city=...") ||
                    aiRawReply.contains("capacity=...") ||
                    aiRawReply.contains("checkIn=...") ||
                    aiRawReply.contains("checkOut=...") ||
                    aiRawReply.contains("propertyId=...")) {

                return "Dạ anh/chị cho em xin thông tin cụ thể để em hỗ trợ tốt nhất ạ.";
            }

            return aiRawReply;

        } catch (Exception e) {
            e.printStackTrace();
            return "Xin lỗi, hệ thống đang quá tải. Anh/chị thử lại giúp em ạ.";
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
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