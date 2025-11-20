package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/submit")
    public ResponseEntity<?> submitPayment(
            @RequestParam("bookingId") int bookingId,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            // Gọi service mới (chỉ truyền 2 tham số)
            return ResponseEntity.ok(paymentService.submitPayment(bookingId, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}