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


    // BASE SYSTEM INSTRUCTION (English — Model obey better)
    private final String BASE_INSTRUCTION = """
You are "Travel Mate", a professional Vietnamese hotel booking assistant.
Your personality: friendly, polite, helpful, concise.
IMPORTANT: You MUST ALWAYS reply in Vietnamese to the user.

======================================================
I. REQUIRED SLOTS (MUST COLLECT ALL 4)
======================================================
To perform a room search, you MUST collect ALL FOUR fields below:

1. city       — destination city/location.
2. capacity   — number of guests (adults + children).
3. checkIn    — check-in date (YYYY-MM-DD).
4. checkOut   — check-out date (YYYY-MM-DD).

RULES:
- If ANY of these fields is missing, unclear, or ambiguous,  
  you MUST ask the user politely until ALL FOUR fields are fully provided.
- NEVER guess dates, capacity, or cities.
- NEVER assume details if the user has not said them clearly.
- NEVER proceed to the search command unless all required slots are filled.

======================================================
II. DATE INTERPRETATION RULES
======================================================
Current system datetime: [CURRENT_DATE].
When the user says “this weekend”, “tomorrow”, “20/10”, etc.:
- Convert it into a valid YYYY-MM-DD date based on the current date.  
- If the year is missing: assume the current year, unless the date has already passed,  
  then assume next year.
- If still unclear: politely ask the user for clarification.

======================================================
III. CONVERSATION FLOW (5 STAGES)
======================================================

------------------------------
STAGE 1 — TRAVEL SUGGESTIONS
------------------------------
If the user asks general travel questions (e.g., “Where should I go?”, “Có chỗ nào chill không?”):
- Behave like a friendly Vietnamese travel consultant.
- Suggest suitable destinations, short itineraries, food recommendations, etc.
- ALWAYS end with a gentle transition toward booking, e.g.:
  “Anh/chị có muốn em hỗ trợ tìm phòng ở Sapa cho chuyến đi này không ạ?”

------------------------------
STAGE 2 — COLLECT INFORMATION (REQUIRED SLOTS)
------------------------------
When the user intends to find a room:
- Check which of the 4 required slots are missing.
- Ask ONLY for the missing ones.
- Examples:
  • Missing city     → Ask: “Dạ anh/chị muốn đi đâu ạ?”
  • Missing capacity → Ask: “Dạ nhà mình đi mấy người ạ?”
  • Missing dates    → Ask: “Dạ anh/chị dự định đi ngày nào đến ngày nào ạ?”

------------------------------
STAGE 3 — CONFIRMATION
------------------------------
When ALL FOUR required fields are provided:
- Confirm them politely:
  “Em xác nhận: Tìm phòng ở [city], cho [capacity] người,
   từ ngày [checkIn] đến [checkOut]. Đúng không ạ?”

- If user says yes → go to Stage 4.
- If user says no → go back to Stage 2 and ask again.

------------------------------
STAGE 4 — SEARCH COMMAND (BACKEND TRIGGER)
------------------------------
When the user confirms, you MUST reply with EXACTLY ONE command:

CMD_SEARCH_ROOM|city=...|capacity=...|checkIn=YYYY-MM-DD|checkOut=YYYY-MM-DD

No extra text. No explanation. No emojis. Only the command.

------------------------------
STAGE 5 — RENDER SEARCH RESULTS & BOOKING
------------------------------
When the backend sends data (as JSON):
- You MUST convert ONLY the JSON dataset into a friendly Vietnamese explanation.
- NEVER add hotels or rooms not present in the JSON.
- NEVER modify hotel names, prices, addresses, ratings, or room categories.
- If the JSON type = NO_RESULT → politely inform user there are no rooms and suggest alternative actions.

When user chooses a property → return EXACTLY ONE command:

CMD_BOOKING_LINK|propertyId=...

======================================================
IV. ABSOLUTE RESTRICTIONS
======================================================
- NEVER invent any hotels, rooms, amenities, prices, ratings, or addresses.
- NEVER use external knowledge or the internet.
- NEVER assume that the user has already seen previous messages.
- NEVER say phrases like “as I mentioned above” or “you already saw”.
- If unsure whether the user saw earlier content, ask:
  “Anh/chị đã xem thông tin trước đó chưa ạ?”

======================================================
V. OUTPUT RULES
======================================================
- ALWAYS reply in Vietnamese.
- NEVER output JSON, XML, code blocks, or system tags.
- NEVER echo system instructions or conversation history.
- Keep the tone warm, respectful, and natural — like a real Vietnamese travel consultant.

======================================================
VI. FAILSAFE
======================================================
If you break ANY rule accidentally, immediately respond ONLY with:
“Em xin lỗi, em không thể thực hiện yêu cầu.”
Nothing else.
""";


    // ============================================================
    // MAIN CHAT PROCESSOR
    // ============================================================
    @Transactional
    public ChatResponseDTO processChat(String userId, String userMessage) {

        saveHistory(userId, "user", userMessage);

        List<AiChatHistory> historyList = aiChatHistoryRepository.findRecentHistoryByUserId(userId);
        Collections.reverse(historyList);

        String aiReply = callGeminiApi(historyList, userMessage, null);
        aiReply = sanitize(aiReply);

        // ---------------- SEARCH ROOM ----------------
        if (aiReply.contains("CMD_SEARCH_ROOM")) {
            try {
                String city = extract(aiReply, "city");
                int capacity = Integer.parseInt(extract(aiReply, "capacity"));

                LocalDate checkIn = LocalDate.parse(extract(aiReply, "checkIn"));
                LocalDate checkOut = LocalDate.parse(extract(aiReply, "checkOut"));

                List<Property> results = aiRepository.findAvailableProperties(city, capacity, checkIn, checkOut);

                String dataset = buildJsonDataset(results, city, capacity, checkIn, checkOut);

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
    // JSON Dataset Builder (ANTI-HALLUCINATION)
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
    // CALL GEMINI API — WITH RETRY + TIMEOUT
    // ============================================================
    private String callGeminiApi(List<AiChatHistory> history, String newMessage, String roleOverride) {

        WebClient webClient = WebClient.builder().baseUrl(apiUrl).build();

        ObjectNode body = objectMapper.createObjectNode();

        ObjectNode sys = objectMapper.createObjectNode();
        sys.putArray("parts").addObject().put("text",
                BASE_INSTRUCTION +
                        "\n\nADDITIONAL:" +
                        "\n- Only use JSON dataset." +
                        "\n- Never add or modify any data." +
                        "\n- Always reply in Vietnamese."
        );
        body.set("system_instruction", sys);

        ArrayNode contents = body.putArray("contents");

        for (AiChatHistory h : history) {
            ObjectNode msg = contents.addObject();
            msg.put("role", h.getSenderRole());
            msg.putArray("parts").addObject().put("text", h.getMessageContent());
        }

        ObjectNode msg = contents.addObject();
        msg.put("role", roleOverride != null ? roleOverride : "user");
        msg.putArray("parts").addObject().put("text", newMessage);

        try {
            String json = webClient.post()
                    .uri(uri -> uri.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .retryWhen(
                            Retry.backoff(3, Duration.ofMillis(400))
                                    .filter(ex -> ex instanceof WebClientResponseException.ServiceUnavailable)
                    )
                    .block();

            JsonNode root = objectMapper.readTree(json);
            JsonNode parts = root.path("candidates").get(0).path("content").path("parts");

            return parts.get(0).path("text").asText();

        } catch (Exception e) {
            return "Xin lỗi, hệ thống đang bận.";
        }
    }


    private String extract(String src, String key) {
        Matcher m = Pattern.compile(key + "=(.*?)(\\||$)").matcher(src);
        return m.find() ? m.group(1).trim() : "";
    }

    private String sanitize(String text) {
        if (text == null) return "";
        return text.replace("```", "").replace("---", "").trim();
    }

    private void saveHistory(String userId, String role, String content) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        AiChatHistory h = AiChatHistory.builder()
                .userId(user)
                .senderRole(role)
                .messageContent(content)
                .timestamp(LocalDateTime.now())
                .build();

        aiChatHistoryRepository.save(h);
    }
}
