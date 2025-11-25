package com.example.smart_booking_system.service;


import com.example.smart_booking_system.dto.ChatResponseDTO;
import com.example.smart_booking_system.entity.AiChatHistory;
import com.example.smart_booking_system.entity.Property;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
/*@RequiredArgsConstructor*/
public class AiService {
    private final AiChatHistoryRepository aiChatHistoryRepository;
    private final UserRepository userRepository;
    private final AiRepository aiRepository;
    private final ObjectMapper objectMapper;


    public AiService(AiChatHistoryRepository aiChatHistoryRepository, UserRepository userRepository,
                     AiRepository aiRepository, ObjectMapper objectMapper){
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


    private final String BASE_INSTRUCTION = """
            BẠN LÀ TRỢ LÝ ẢO CỦA HỆ THỐNG ĐẶT PHÒNG "TRAVELMATE".
            
            --- CẤU TRÚC DỮ LIỆU BẮT BUỘC (REQUIRED SLOTS) ---
            Để tìm phòng, bạn BẮT BUỘC phải điền đầy đủ **4 trường thông tin** sau:
            1. **city**: Tên thành phố/địa điểm.
            2. **capacity**: Số lượng khách (người lớn + trẻ em).
            3. **checkIn**: Ngày nhận phòng (Định dạng YYYY-MM-DD).
            4. **checkOut**: Ngày trả phòng (Định dạng YYYY-MM-DD).

            --- QUY TẮC XỬ LÝ NGÀY THÁNG ---
            - Hôm nay là: [CURRENT_DATE].
            - Nếu khách nói "Cuối tuần này", "Ngày mai", "20/10"... hãy dựa vào ngày hôm nay để quy đổi ra ngày cụ thể YYYY-MM-DD.
            - Nếu khách không nói rõ năm, hãy mặc định là năm hiện tại hoặc năm sau (nếu tháng đã qua).
            - Nếu không hiểu thì phải hỏi cho tới khi hiểu thì thôi.

            --- QUY TRÌNH HỘI THOẠI ---
            **GIAI ĐOẠN 1: THU THẬP (Hỏi thiếu - Đáp đủ)**
            - Thiếu `city` -> Hỏi địa điểm.
            - Thiếu `capacity` -> Hỏi số người.
            - Thiếu ngày (`checkIn`/`checkOut`) -> Hỏi: "Anh/chị dự định đi ngày nào đến ngày nào ạ?"
            - Tuyệt đối không tự bịa ngày.

            **GIAI ĐOẠN 2: XÁC NHẬN**
            - Khi đủ 4 thông tin, xác nhận lại: "Em xác nhận: Tìm phòng ở [city], cho [capacity] người, từ ngày [checkIn] đến [checkOut]. Đúng không ạ?"

            **GIAI ĐOẠN 3: THỰC THI**
            - Khách đồng ý -> Trả về lệnh DUY NHẤT:
              `CMD_SEARCH_ROOM|city=...|capacity=...|checkIn=YYYY-MM-DD|checkOut=YYYY-MM-DD`

            **GIAI ĐOẠN 4 & 5**: Tư vấn và Chốt đơn (như cũ).
              Booking link command: `CMD_BOOKING_LINK|propertyId=...`
            """;

    @Transactional
    public ChatResponseDTO processChat(String userId, String userMessage) {
        //lưu lịch sử
        saveHistory(userId, "user", userMessage);

        //lấy lịch sử 20 chat gần nhất
        List<AiChatHistory> historyList = aiChatHistoryRepository.findRecentHistoryByUserId(userId);
        Collections.reverse(historyList);

        //gọi Model
        String aiReply = callGeminiApi(historyList, userMessage, null);
        System.out.println("🟥 [DEBUG] AI Reply Raw: " + aiReply);

        //xử lý logic
        //tìm phòng
        if (aiReply.contains("CMD_SEARCH_ROOM")) {
            try {
                String city = extractValue(aiReply, "city");
                int capacity = Integer.parseInt(extractValue(aiReply, "capacity"));

                // Xử lý ngày (nếu AI trả về checkIn, checkOut)
                String checkInStr = extractValue(aiReply, "checkIn");
                String checkOutStr = extractValue(aiReply, "checkOut");

                LocalDate checkIn = LocalDate.now();
                LocalDate checkOut = LocalDate.now().plusDays(1);

                if(!checkInStr.isEmpty()) checkIn = LocalDate.parse(checkInStr);
                if(!checkOutStr.isEmpty()) checkOut = LocalDate.parse(checkOutStr);

                // 🟥 LOG 2: Xem tham số tìm kiếm là gì
                System.out.println("🟥 [DEBUG] Searching -> City: " + city + ", Cap: " + capacity + ", In: " + checkIn + ", Out: " + checkOut);

                // Gọi Repo
                List<Property> results = aiRepository.findAvailableProperties(city, capacity, checkIn, checkOut);

                // 🟥 LOG 3: Xem tìm được bao nhiêu kết quả
                System.out.println("🟥 [DEBUG] Found Results Size: " + results.size());
                if (!results.isEmpty()) {
                    System.out.println("🟥 [DEBUG] First Result: " + results.get(0).getPropertyName());
                }

                String dataContext = buildDataContext(results, city, capacity, checkIn, checkOut);

                // 🟥 LOG 4: Xem dữ liệu gửi ngược lại cho AI
                System.out.println("🟥 [DEBUG] Data Context sent to AI: " + dataContext);

                String finalReply = callGeminiApi(historyList, dataContext, "model");
                saveHistory(userId, "model", finalReply);
                return new ChatResponseDTO(finalReply);

            } catch (Exception e) {
                e.printStackTrace();
                // ... xử lý lỗi
            }
        }
            // Booking Link
        if (aiReply.contains("CMD_BOOKING_LINK")) {
            String propIdStr = extractValue(aiReply, "propertyId");
            String bookingLink = frontendUrl + "/property-details/" + propIdStr;
            String finalReply = "Dạ em đã tạo hồ sơ đặt phòng. Anh/chị nhấn vào đây để hoàn tất nhé: <br>" +
                    "<a href='" + bookingLink + "' target='_blank'>👉 <b>Đặt phòng ngay</b></a>";
            saveHistory(userId, "model", finalReply);
            return new ChatResponseDTO(finalReply);
        }

        // C. Chat thường
        saveHistory(userId, "model", aiReply);
        return new ChatResponseDTO(aiReply);

    }

    // Helper: Tách giá trị từ lệnh
    private String extractValue(String source, String key) {
        try {
            Pattern pattern = Pattern.compile(key + "=(.*?)(?:\\||$)");
            Matcher matcher = pattern.matcher(source);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) { return ""; }
        return "";
    }

    // Helper: Tạo văn bản mô tả kết quả
    // Helper: Tạo văn bản mô tả kết quả
    // Helper: Tạo văn bản mô tả kết quả (FORMAT ĐẸP & TỰ NHIÊN)
    private String buildDataContext(List<Property> properties, String city, int capacity, LocalDate in, LocalDate out) {
        // Trường hợp 1: Không tìm thấy
        if (properties.isEmpty()) {
            return "HỆ THỐNG: Đã tìm kiếm nhưng KHÔNG CÓ phòng nào trống tại " + city +
                    " cho " + capacity + " người từ ngày " + in + " đến " + out + ".\n" +
                    "YÊU CẦU: Hãy báo lại cho khách tin buồn này một cách khéo léo, và gợi ý khách thử đổi ngày hoặc tìm địa điểm lân cận.";
        }

        // Trường hợp 2: Có dữ liệu -> Format đẹp
        StringBuilder sb = new StringBuilder();

        // Câu lệnh "thôi miên" AI để nó không nói linh tinh
        sb.append("HỆ THỐNG: Đã tìm thấy ").append(properties.size()).append(" kết quả tốt nhất. ");
        sb.append("NHIỆM VỤ CỦA BẠN: Hãy trả lời khách hàng bằng giọng điệu niềm nở, và hiển thị danh sách bên dưới Y HỆT format này (dùng HTML để in đậm):\n\n");

        int index = 1;
        for (Property p : properties.subList(0, Math.min(properties.size(), 5))) {
            // Dòng 1: Tên + Rating
            sb.append(index++).append(". <b>").append(p.getPropertyName()).append("</b> (").append(p.getRating()).append(" sao)\n");

            // Dòng 2: Đánh giá chi tiết
            sb.append("   - Đánh giá: ").append(p.getRating()).append(" ⭐ (").append(p.getReviewCount()).append(" review)\n");

            // Dòng 3: Địa chỉ
            sb.append("   - Địa chỉ: ").append(p.getAddress()).append("\n");

            // Dòng 4: Các phòng trống (Dùng stream để nối chuỗi)
            sb.append("   - Các phòng trống: ");
            List<String> roomInfos = p.getRooms().stream()
                    .filter(r -> r.isActive() && r.getCapacity() >= capacity) // Lọc phòng phù hợp
                    .map(r -> String.format("%s (Giá: %,.0f VNĐ)", r.getRoomName(), r.getPricePerNight())) // Format số tiền có dấu phẩy
                    .toList();

            sb.append(String.join(", ", roomInfos)); // Nối các phòng bằng dấu phẩy
            sb.append(".\n\n"); // Xuống dòng cách đoạn
        }

        sb.append("(Cuối cùng, hãy hỏi khách: 'Anh/chị ưng ý chỗ nào để em hỗ trợ đặt phòng luôn ạ?')");

        return sb.toString();
    }

    //Hàm gọi model nhé
    private String callGeminiApi(List<AiChatHistory> history, String newMessage, String roleOverride){

        WebClient webClient = WebClient.create();
        ObjectNode requestBody = objectMapper.createObjectNode();

        //cho ngày hiện tại vào prompt
        String todayStr = LocalDateTime.now().toString();
        String finalInstruction = BASE_INSTRUCTION.replace("[CURRENT_DATE]", todayStr);

        ObjectNode systemInst = objectMapper.createObjectNode();
        systemInst.putArray("parts").addObject().put("text", finalInstruction);
        requestBody.set("system_instruction", systemInst);

        ArrayNode contents = requestBody.putArray("contents");
        for (AiChatHistory h : history) {
            ObjectNode msgNode = contents.addObject();
            msgNode.put("role", h.getSenderRole());
            msgNode.putArray("parts").addObject().put("text", h.getMessageContent());
        }

        ObjectNode msgNode = contents.addObject();
        msgNode.put("role", roleOverride != null ? roleOverride : "user");
        msgNode.putArray("parts").addObject().put("text", newMessage);

        try {
            String responseJson = webClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseJson);
            if (root.has("candidates") && root.path("candidates").size() > 0) {
                return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Xin lỗi, hệ thống đang bận.";
    }




    private void saveHistory(String userId, String role, String content) {
        var user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            AiChatHistory history = AiChatHistory.builder().userId(user).senderRole(role).messageContent(content).timestamp(LocalDateTime.now()).build();
            aiChatHistoryRepository.save(history);
        }
    }
}
