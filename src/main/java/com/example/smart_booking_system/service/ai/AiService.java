package com.example.smart_booking_system.service.ai;

import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Room;
import com.example.smart_booking_system.repository.PropertyDetailRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final UserRepository userRepository;
    private final PropertyDetailRepository propertyDetailRepository;
    private final ObjectMapper objectMapper;
    private final BookingSessionManager sessionManager;

    private WebClient webClient;

    public AiService(
            UserRepository userRepository,
            PropertyDetailRepository propertyDetailRepository,
            ObjectMapper objectMapper,
            BookingSessionManager sessionManager
    ) {
        this.userRepository = userRepository;
        this.propertyDetailRepository = propertyDetailRepository;
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
    }

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Prompt V3 (tối ưu mạnh, cực ngắn nhưng rõ ràng)
     * - 3 chế độ: Booking (backend-only), Travel Guide, Q&A
     * - Cấm markdown, cấm *, cấm tự sinh CMD
     * - Có self-check trước khi trả ra
     */
    private final String BASE_INSTRUCTION = """
    You are Travel Mate, a Vietnamese travel & hotel assistant.

    === MODES ===
    1) BOOKING MODE (backend-controlled):
       - Only active when backend says booking mode on.
       - Do not provide travel suggestions.
       - Do not switch mode yourself.

    2) TRAVEL GUIDE MODE:
       - Triggered when user asks về điểm đến, đi đâu chơi, review địa phương.
       - Output MUST contain exactly 2 fields:

         RESPONSE (text, natural, emotional, inspiring)
         PAYLOAD (pure JSON, no extra words)

       Example:
       RESPONSE:
       Hà Giang là điểm đến tuyệt vời với cảnh sắc hùng vĩ. 
       Dưới đây là những nơi nổi bật mà bạn nên ghé thăm:

       PAYLOAD (JSON only):
       {
         "city": "Hà Giang",
         "places": [
            { "name": "Cột cờ Lũng Cú", "type": "landmark" },
            { "name": "Đèo Mã Pì Lèng", "type": "mountain" },
            { "name": "Cao nguyên đá Đồng Văn", "type": "heritage" }
         ]
       }

       RULES:
       - RESPONSE phải giàu cảm xúc, dễ đọc, không robot.
       - PAYLOAD phải là JSON thật, không được thêm chữ nào khác.
       - Không dùng markdown (*, #).
       - Không bullet trong RESPONSE (trừ khi thật sự cần).

    3) Q&A MODE:
       - Giải đáp bình thường.
       - Plain text only.

    === GLOBAL RULES ===
    - Never output markdown (*, **, #).
    - Never hallucinate facts.
    - Keep output short, friendly, natural.

    === SELF CHECK BEFORE RESPOND ===
    - Ensure correct MODE.
    - Ensure JSON is valid.
    - Ensure RESPONSE and PAYLOAD tách riêng hoàn toàn.
    """;


    @PostConstruct
    private void init() {
        webClient = WebClient.builder().baseUrl(apiUrl).build();
    }

    // ==========================================================
    // MAIN ENTRY — Backend controls booking mode completely
    // ==========================================================
    @Transactional
    public ChatResponseDTO processChat(String userId, String userMessage) {

        if (userMessage == null || userMessage.isEmpty()) {
            return new ChatResponseDTO("Dạ anh/chị gửi nội dung giúp em ạ.", "NORMAL_CHAT", null);
        }

        String clean = preprocess(userMessage);
        BookingSession session = sessionManager.getOrCreate(userId);

        // ---------------- BOOKING FLOW ----------------
        if (session.isActive()) {
            return handleBookingFlow(userId, session, clean);
        }

        // If message looks like booking request → enable booking mode backend side
        if (detectBookingIntent(clean)) {
            session.setActive(true);

            Integer cap = extractCapacity(clean);
            String city = extractCity(clean);

            if (city != null) session.setCity(city);
            if (cap != null) session.setCapacity(cap);

            if (session.getCity() == null) {
                session.setStep(BookingSession.Step.WAITING_CITY);
                return new ChatResponseDTO("Bạn muốn tìm phòng ở thành phố nào ạ?", "NORMAL_CHAT", null);
            }
            if (session.getCapacity() == null) {
                session.setStep(BookingSession.Step.WAITING_CAPACITY);
                return new ChatResponseDTO("Bạn muốn tìm phòng cho bao nhiêu người ạ?", "NORMAL_CHAT", null);
            }

            session.setStep(BookingSession.Step.READY);
            return executeSearch(userId, session.getCity(), session.getCapacity(), session);
        }

        // ---------------- NORMAL CHAT (Travel / Q&A) ----------------
        String aiReply = callGemini(clean);
        if (aiReply == null) {
            return new ChatResponseDTO("Xin lỗi, em chưa hiểu ý anh/chị ạ.", "NORMAL_CHAT", null);
        }

        String responseText = aiReply;
        JsonNode payloadJson = null;

// Tách RESPONSE và JSON PAYLOAD
        int payloadIndex = aiReply.indexOf("{");
        if (payloadIndex > 0) {
            responseText = aiReply.substring(0, payloadIndex).trim();
            String jsonPart = aiReply.substring(payloadIndex).trim();

            try {
                payloadJson = objectMapper.readTree(jsonPart);
            } catch (Exception ignored) {}
        }

        responseText = sanitize(responseText);

        return new ChatResponseDTO(responseText, "NORMAL_CHAT", payloadJson);

    }

    // ==========================================================
    // BOOKING FLOW HANDLER
    // ==========================================================
    private ChatResponseDTO handleBookingFlow(String userId, BookingSession session, String msg) {

        // --- CASE: User đổi ý, đưa city mới ---
        String newCity = CityExtractor.extractCity(msg);
        if (newCity != null) {
            session.setCity(newCity);
            session.setStep(BookingSession.Step.WAITING_CAPACITY);

            return new ChatResponseDTO(
                    "Dạ mình chuyển sang tìm phòng ở " + newCity + " ạ. Bạn muốn tìm phòng cho bao nhiêu người ạ?",
                    "NORMAL_CHAT",
                    null
            );
        }

        // --- CASE: User đổi ý capacity trong lúc chờ capacity ---
        Integer newCap = extractCapacity(msg);
        if (newCap != null && newCap > 0 && session.getStep() == BookingSession.Step.WAITING_CAPACITY) {
            session.setCapacity(newCap);
            session.setStep(BookingSession.Step.READY);
            return executeSearch(userId, session.getCity(), newCap, session);
        }

        // --- CASE: Waiting city ---
        if (session.getStep() == BookingSession.Step.WAITING_CITY) {
            if (newCity != null) {
                session.setCity(newCity);
                session.setStep(BookingSession.Step.WAITING_CAPACITY);
                return new ChatResponseDTO("Bạn muốn tìm phòng cho bao nhiêu người ạ?", "NORMAL_CHAT", null);
            }
            return new ChatResponseDTO("Anh/chị cho em biết thành phố mình muốn tìm phòng ạ?", "NORMAL_CHAT", null);
        }

        // --- CASE: Waiting capacity ---
        if (session.getStep() == BookingSession.Step.WAITING_CAPACITY) {
            if (newCap != null && newCap > 0) {
                session.setCapacity(newCap);
                session.setStep(BookingSession.Step.READY);
                return executeSearch(userId, session.getCity(), newCap, session);
            }
            return new ChatResponseDTO("Anh/chị cho em biết số lượng người ạ?", "NORMAL_CHAT", null);
        }

        // --- CASE: READY ---
        if (session.getStep() == BookingSession.Step.READY) {
            return executeSearch(userId, session.getCity(), session.getCapacity(), session);
        }

        // fallback (không nên xảy ra)
        session.reset();
        return new ChatResponseDTO("Mình bắt đầu lại giúp em được không ạ?", "NORMAL_CHAT", null);
    }

    // ==========================================================
    // EXECUTE SEARCH + RETURN RESULT
    // ==========================================================
    private ChatResponseDTO executeSearch(String userId, String cityRaw, int capacity, BookingSession session) {
        String normalizedCity = normalizeCity(cityRaw);

        List<Property> results = propertyDetailRepository.findAvailableProperties(normalizedCity, capacity);
        String datasetJson = buildJsonDataset(results, cityRaw, capacity);

        String reply = results.isEmpty()
                ? "Dạ hiện tại em chưa tìm được chỗ phù hợp ạ."
                : String.format("Dạ em đã tìm được %d chỗ ở phù hợp tại %s cho %d người ạ.", results.size(), cityRaw, capacity);

        session.reset();

        JsonNode json = null;
        try {
            json = objectMapper.readTree(datasetJson);
        } catch (Exception ignored) {}

        return new ChatResponseDTO(reply, "SEARCH_ROOM_RESULT", json);
    }

    // ==========================================================
    // CALL GEMINI (Compact)
    // ==========================================================
    private String callGemini(String userMessage) {
        try {
            ObjectNode req = objectMapper.createObjectNode();

            ObjectNode sys = objectMapper.createObjectNode();
            sys.putArray("parts").addObject().put("text",
                    BASE_INSTRUCTION +
                            "\nToday: " + LocalDateTime.now()
            );
            req.set("system_instruction", sys);

            ArrayNode contents = req.putArray("contents");
            ObjectNode msg = contents.addObject();
            msg.put("role", "user");
            msg.putArray("parts").addObject().put("text", userMessage);

            String raw = webClient.post()
                    .uri(uri -> uri.queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(req.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)))
                    .block();

            JsonNode root = objectMapper.readTree(raw);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText("");

        } catch (Exception e) {
            return "Xin lỗi anh/chị, hệ thống đang quá tải ạ.";
        }
    }

    // ==========================================================
    // HELPERS
    // ==========================================================
    private String preprocess(String s) {
        return s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String sanitize(String s) {
        return s.replace("```", "").replace("---", "").trim();
    }

    private String enforceFormat(String text) {
        if (!text.contains(":")) return text; // Q&A mode
        String[] lines = text.split("\n");
        StringBuilder out = new StringBuilder();
        boolean lastWasTitle = false;

        for (String line : lines) {
            String l = line.trim();

            if (l.endsWith(":")) {
                lastWasTitle = true;
                out.append(l).append("\n");
                continue;
            }
            if (lastWasTitle) {
                out.append("- ").append(l).append("\n");
                lastWasTitle = false;
            } else {
                out.append(l).append("\n");
            }
        }
        return out.toString().trim();
    }

    private boolean detectBookingIntent(String text) {
        String s = text.toLowerCase();
        return s.matches(".*(tìm phòng|đặt phòng|thuê phòng|book).*")
                || s.matches(".*\\d+\\s*(người|khách).*");
    }

    private Integer extractCapacity(String text) {
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return null;
    }

    private String extractCity(String text) {
        Matcher m = Pattern.compile("\\b(?:ở|tại)\\s+([\\p{L}\\s]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1).trim();
        if (text.length() <= 25 && !text.contains(" ")) return text;
        return null;
    }

    private String normalizeCity(String s) {
        s = s.toLowerCase().trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("\\b(thanh pho|tinh|tp|tp\\.)\\b", "");
        return s.replaceAll("[^a-z0-9\\s]", "").trim();
    }

    private String buildJsonDataset(List<Property> properties, String city, int capacity) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("city", city);
        root.put("capacity", capacity);

        if (properties.isEmpty()) {
            root.put("type", "NO_RESULT");
            return root.toString();
        }

        root.put("type", "RESULT");
        ArrayNode arr = root.putArray("properties");

        for (Property p : properties) {
            ObjectNode node = arr.addObject();

            node.put("propertyId", p.getPropertyId());
            node.put("name", p.getPropertyName());
            node.put("rating", p.getRating() != null ? p.getRating().doubleValue() : 0);
            node.put("reviewCount", p.getReviewCount());
            node.put("address", p.getAddress());
            node.put("bookingUrl", frontendUrl + "/hotels/" + p.getPropertyId());

            ArrayNode rooms = node.putArray("rooms");

            p.getRooms().stream()
                    .filter(Room::isActive)
                    .filter(r -> r.getCapacity() >= capacity)
                    .sorted(Comparator.comparing(r -> r.getPricePerNight()))
                    .limit(3)
                    .forEach(r -> {
                        ObjectNode rn = rooms.addObject();
                        rn.put("roomName", r.getRoomName());
                        rn.put("capacity", r.getCapacity());
                        rn.put("price", r.getPricePerNight().longValue());
                    });
        }

        return root.toString();
    }
}
