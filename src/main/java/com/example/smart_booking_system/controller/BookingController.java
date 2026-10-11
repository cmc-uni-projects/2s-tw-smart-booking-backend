package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final Messages messages;

    private final BookingService bookingService;


    // ==========================================
    // CREATE BOOKING
    // ==========================================
    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO req) {
        try {
            BookingResponseDTO result = bookingService.createBooking(req);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    // ==========================================
    // CANCEL BOOKING
    // ==========================================
    @PutMapping("/cancel/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable int bookingId) {
        // Logic mới: Hủy và tự động tạo RefundRequest bên trong Service
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    // ==========================================
    // ADMIN APPROVE REFUND (Duyệt hoàn tiền)
    // ==========================================
    @PutMapping("/approve-refund/{bookingId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveRefund(@PathVariable int bookingId) {
        try {
            bookingService.approveRefund(bookingId);
            return ResponseEntity.ok(messages.t("api.refund.approvedAndMailed"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    // ==========================================
    // GET BOOKING BY ID
    // ==========================================
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable int bookingId) {
        try {
            return ResponseEntity.ok(bookingService.getBookingById(bookingId));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    // ==========================================
    // GET ALL BOOKINGS OF USER
    // ==========================================
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable String userId) {
        try {
            List<BookingResponseDTO> list = bookingService.getBookingsByUserId(userId);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    // ==========================================
    // GET ALL BOOKINGS OF PROPERTY
    // ==========================================
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getBookingsByProperty(@PathVariable int propertyId) {
        try {
            List<BookingResponseDTO> list = bookingService.getBookingsByPropertyId(propertyId);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    // ==========================================
    // GET ALL BOOKINGS (ADMIN)
    // ==========================================
    @GetMapping("")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings() {
        try {
            return ResponseEntity.ok(bookingService.getAllBookings());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    @PutMapping("/{bookingId}/check-in")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkIn(@PathVariable int bookingId) {
        bookingService.checkInBooking(bookingId);
        return ResponseEntity.ok(messages.t("api.booking.checkedIn"));
    }

    // Check-out (Mới - Thay thế logic checkout cũ)
    @PutMapping("/checkout/{bookingId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkOut(@PathVariable int bookingId) {
        try {
            bookingService.checkOutBooking(bookingId);
            return ResponseEntity.ok(messages.t("api.booking.checkedOut"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(messages.of(ex));
        }
    }

    @PutMapping("/{bookingId}/apply-promotion")
    @PreAuthorize("hasRole('CUSTOMER')") // Chỉ khách hàng mới đc nhập
    public ResponseEntity<?> applyPromotion(
            @PathVariable int bookingId,
            @RequestParam String code
    ) {
        try {
            BookingResponseDTO result = bookingService.applyPromotion(bookingId, code);
            return ResponseEntity.ok(ApiResponse.success(messages.t("api.promotion.applied"), result));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messages.of(ex)));
        }
    }

    @GetMapping("/room/{roomId}/availability")
    public ResponseEntity<ApiResponse> getRoomAvailability(@PathVariable int roomId) {
        List<Map<String, String>> occupiedDates = bookingService.getRoomAvailability(roomId);
        return ResponseEntity.ok(ApiResponse.success(messages.t("api.booking.availability"), occupiedDates));    }


    @PutMapping("/{bookingId}/remove-promotion")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')") // Cho phép cả Khách và Admin bỏ mã
    public ResponseEntity<?> removePromotion(
            @PathVariable int bookingId,
            @RequestParam String code // Mã muốn bỏ (để biết bỏ mã nào)
    ) {
        try {
            // Gọi sang Service để xử lý
            BookingResponseDTO result = bookingService.removePromotion(bookingId, code);
            return ResponseEntity.ok(ApiResponse.success(messages.t("api.promotion.removed"), result));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messages.of(ex)));
        }
    }
}
