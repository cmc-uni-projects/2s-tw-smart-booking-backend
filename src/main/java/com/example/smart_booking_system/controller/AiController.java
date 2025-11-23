package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.*;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final UserRepository userRepository;

    public AiController(AiService aiService, UserRepository userRepository) {
        this.aiService = aiService;
        this.userRepository = userRepository;
    }


    @PostMapping("/ask")
    public String ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        return aiService.askAboutProperties(question);
    }


    @GetMapping("/suggest")
    public List<PropertySimpleDTO> suggest() {
        return aiService.suggestProperties();
    }



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
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        return aiService.bookRoom(
                req.getRoomId(),
                LocalDate.parse(req.getCheckIn()),
                LocalDate.parse(req.getCheckOut()),
                user
        );
    }


}
