package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.*;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepository;

    // ✅ Endpoint Chat duy nhất: Xử lý hỏi đáp & gợi ý
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Bạn muốn hỏi gì về TravelMate?");
        }
        return ResponseEntity.ok(aiService.chatWithAI(question));
    }

    // Các API booking/check giữ nguyên
    @PostMapping("/available")
    public List<RoomSimpleDTO> available(@RequestBody AiAvailableRequest req) {
        return aiService.getAvailableRooms(
                req.getCity(),
                LocalDate.parse(req.getCheckIn()),
                LocalDate.parse(req.getCheckOut())
        );
    }

    @PostMapping("/book")
    public BookingSimpleDTO book(@RequestBody AiBookingRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return aiService.bookRoom(
                req.getRoomId(),
                LocalDate.parse(req.getCheckIn()),
                LocalDate.parse(req.getCheckOut()),
                user
        );
    }
}