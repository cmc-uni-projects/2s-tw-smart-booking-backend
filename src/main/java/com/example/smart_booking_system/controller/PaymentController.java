package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * User submit bằng chứng thanh toán (Ảnh chụp màn hình)
     */
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    public ResponseEntity<?> submitPayment(
            @RequestParam("bookingId") int bookingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            return ResponseEntity.ok(paymentService.submitPayment(bookingId, file, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin duyệt hoặc từ chối thanh toán
     * Body JSON: { "status": "APPROVE" (hoặc "REJECT"), "reason": "..." }
     */
    @PostMapping("/review/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> reviewPayment(
            @PathVariable int paymentId,
            @RequestBody Map<String, String> body
    ) {
        try {
            String status = body.get("status"); // "APPROVE" hoặc "REJECT"
            String reason = body.get("reason");

            boolean isApproved = "APPROVE".equalsIgnoreCase(status);

            return ResponseEntity.ok(paymentService.reviewPayment(paymentId, isApproved, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}