package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            return ResponseEntity.ok(paymentService.submitPayment(bookingId, note));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserHistory(@PathVariable String userId) {
        return ResponseEntity.ok(paymentService.getUserTransactionHistory(userId));
    }

    @PostMapping("/refund/{bookingId}")
    public ResponseEntity<?> refundPayment(@PathVariable int bookingId) {
        try {
            return ResponseEntity.ok(paymentService.processRefund(bookingId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/request-refund/{bookingId}")
    public ResponseEntity<?> requestRefund(
            @PathVariable int bookingId,
            @RequestParam("reason") String reason // ✅ Thêm tham số này
    ) {
        try {
            return ResponseEntity.ok(paymentService.requestRefundByUser(bookingId, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTransactions() {
        try {
            return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu thành công", paymentService.getAllTransactions()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}