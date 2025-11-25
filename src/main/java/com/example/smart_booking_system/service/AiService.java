package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.entity.AiChatHistory;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.repository.AiChatHistoryRepository;
import com.example.smart_booking_system.repository.AiRepository;
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
    private final AiRepository aiRepository;
    private final ObjectMapper objectMapper;

    public AiService(AiChatHistoryRepository aiChatHistoryRepository,
                     UserRepository userRepository,
                     AiRepository aiRepository,
                     ObjectMapper objectMapper) {

        this.aiChatHistoryRepository = aiChatHistoryRepository;
        this.userRepository = userRepository;
        this.aiRepository = aiRepository;
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
I. REQUIRED SLOTS (MUST COLLECT ALL 4)
======================================================
To perform a room search, you MUST collect ALL FOUR fields:
1. city       — destination city/location.
2. capacity   — number of guests (adults + children).
3. checkIn    — check-in date (YYYY-MM-DD).
4. checkOut   — check-out date (YYYY-MM-DD).

RULES:
- If ANY slot is missing or unclear, politely ask the user.
- NEVER guess or assume details.
- NEVER proceed to search unless all 4 slots are confirmed.

======================================================
II. DATE INTERPRETATION RULES
======================================================
Current system datetime: [CURRENT_DATE].
When the user says things like:
- “ngày mai”
- “cuối tuần này”
- “20/10”
You MUST convert it into a real YYYY-MM-DD based on current date.
If unclear → ask the user again.

======================================================
III. CONVERSATION FLOW
======================================================

STAGE 1 — Travel Suggestions  
STAGE 2 — Collect Required Fields  
STAGE 3 — Confirm All 4 Fields  
STAGE 4 — Output ONLY:
CMD_SEARCH_ROOM|city=...|capacity=...|checkIn=...|checkOut=...

IMPORTANT - DO NOT EXPOSE COMMANDS:
- Khi xuất lệnh CMD_SEARCH_ROOM hoặc CMD_BOOKING_LINK, bạn CHỈ gửi lệnh đó cho hệ thống (backend).
- TUYỆT ĐỐI KHÔNG ĐƯỢC hiển thị các lệnh CMD_SEARCH_ROOM hoặc CMD_BOOKING_LINK cho người dùng.
- KHÔNG ĐƯỢC nói hoặc gợi ý nội dung dạng: “Em sẽ gửi lệnh”, “CMD_SEARCH_ROOM là…”, “Dưới đây là lệnh tìm phòng...”.
- Nếu bạn chuẩn bị nói nội dung của lệnh cho người dùng → HÃY DỪNG LẠI và thay vào đó hỏi xác nhận:
  “Dạ anh/chị vui lòng xác nhận giúp em trước khi em tiến hành tìm phòng ạ?”
- Chỉ backend mới được nhìn thấy lệnh CMD_, người dùng KHÔNG BAO GIỜ được thấy.


STAGE 5 — When backend sends JSON dataset:
- Convert ONLY that JSON to a polite Vietnamese explanation.
- NEVER add hotels, rooms, addresses, ratings, or data not present.
- If JSON.type = NO_RESULT → politely inform user.

======================================================
IV. HARD RESTRICTIONS
======================================================
- You MUST NOT invent anything.
- You MUST NOT use external knowledge.
- You MUST NOT assume user saw earlier messages.
- If unsure, ask: “Anh/chị đã xem thông tin trước đó chưa ạ?”

IMPORTANT HARD RULE:
- You must NEVER display or mention the template examples such as:
- CMD_SEARCH_ROOM|city=...|capacity=...|checkIn=...|checkOut=...
- CMD_BOOKING_LINK|propertyId=...

- These templates are ONLY for you to use internally when you output real commands.
- You must NEVER show them to the user unless you are outputting a REAL command.
- If the user has not provided enough info, do NOT output any CMD_* and do NOT show the examples.


======================================================
V. FAILSAFE
======================================================
Only use the failsafe if you are 100% certain that the user's request forces you to break the rules.
Do NOT trigger the failsafe during normal conversation or booking flow.

======================================================
VI. BOOKING WHEN ROOM NAME IS MENTIONED
======================================================
If the user mentions a room name (e.g., “Classic Queen”, “Deluxe”, “Suite”)
BUT you do NOT currently have a JSON dataset from the backend:

- You MUST NOT assume the property or the room.
- You MUST NOT invent any information.
- You MUST NOT activate failsafe.
- Instead, politely ask the user:

  “Dạ anh/chị ơi, để em kiểm tra được phòng này thì anh/chị cho em xin lại thông tin tìm phòng hoặc em gửi lại danh sách phòng để mình chọn ạ?”

Only when a JSON dataset exists AND the room name matches exactly one of the rooms in the dataset:
- Return EXACTLY one booking command:

  CMD_BOOKING_LINK|propertyId=...

""";

    // ============================================================
    // MAIN CHAT PROCESSOR
    // ============================================================
    @Transactional
    public ChatResponseDTO processChat(String userId, String userMessage) {

        saveHistory(userId, "user", userMessage);

        List<AiChatHistory> historyList =
                aiChatHistoryRepository.findRecentHistoryByUserId(userId);
        Collections.reverse(historyList);

        // STEP 1 → send user message to Gemini
        String aiReply = callGeminiApi(historyList, userMessage, null);
        aiReply = sanitize(aiReply);
        // ---------------- SEARCH ROOM ----------------
        if (aiReply.contains("CMD_SEARCH_ROOM")) {
            try {
                String city = extract(aiReply, "city");
                int capacity = Integer.parseInt(extract(aiReply, "capacity"));

                LocalDate checkIn = LocalDate.parse(extract(aiReply, "checkIn"));
                LocalDate checkOut = LocalDate.parse(extract(aiReply, "checkOut"));

                // Query DB
                List<Property> results =
                        aiRepository.findAvailableProperties(city, capacity, checkIn, checkOut);

                // Convert DB result to JSON dataset
                String dataset =
                        buildJsonDataset(results, city, capacity, checkIn, checkOut);

                // Ask AI to render the JSON dataset to Vietnamese explanation
                String finalReply = callGeminiApi(historyList, dataset, "model");

                saveHistory(userId, "model", finalReply);
                return new ChatResponseDTO(finalReply);

            } catch (Exception e) {
                String err = "Xin lỗi anh/chị, hệ thống gặp lỗi khi tìm phòng. Anh/chị thử lại giúp em nhé.";
                saveHistory(userId, "model", err);
                return new ChatResponseDTO(err);
            }
        }

        // ---------------- BOOKING LINK ----------------
        if (aiReply.contains("CMD_BOOKING_LINK")) {

            String propIdStr = extract(aiReply, "propertyId");
            String link = frontendUrl + "/property-details/" + propIdStr;

            String reply =
                    "Dạ em đã tạo hồ sơ đặt phòng rồi ạ ❤️\n" +
                            "Anh/chị nhấn vào link này để hoàn tất:\n" +
                            link;

            saveHistory(userId, "model", reply);
            return new ChatResponseDTO(reply);
        }

        // ---------------- NORMAL CHAT ----------------
        saveHistory(userId, "model", aiReply);
        return new ChatResponseDTO(aiReply);
    }


    // ============================================================
    // JSON DATASET BUILDER (Anti-hallucination)
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
                        + "\n- Never display template commands such as CMD_SEARCH_ROOM|city=... or CMD_BOOKING_LINK|propertyId=..."
                        + "\n- Only output REAL commands when all required info is confirmed."
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

            // HARD FILTER — ngăn Gemini lộ template lệnh
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

} // END OF CLASS
