package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO req) {
        try {
            BookingResponseDTO result = bookingService.createBooking(req);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Create booking failed: " + ex.getMessage());
        }
    }

    @PutMapping("/cancel/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable int bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @PutMapping("/approve-refund/{bookingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveRefund(@PathVariable int bookingId) {
        try {
            bookingService.approveRefund(bookingId);
            return ResponseEntity.ok("Đã duyệt hoàn tiền và gửi mail thành công!");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Approve refund failed: " + ex.getMessage());
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable int bookingId) {
        try {
            return ResponseEntity.ok(bookingService.getBookingById(bookingId));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Get booking failed: " + ex.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUser(@PathVariable String userId) {
        try {
            List<BookingResponseDTO> list = bookingService.getBookingsByUserId(userId);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Get bookings by user failed: " + ex.getMessage());
        }
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<?> getBookingsByProperty(@PathVariable int propertyId) {
        try {
            List<BookingResponseDTO> list = bookingService.getBookingsByPropertyId(propertyId);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Get bookings by property failed: " + ex.getMessage());
        }
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings() {
        try {
            return ResponseEntity.ok(bookingService.getAllBookings());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Get all bookings failed: " + ex.getMessage());
        }
    }

    @PutMapping("/{bookingId}/check-in")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkIn(@PathVariable int bookingId) {
        bookingService.checkInBooking(bookingId);
        return ResponseEntity.ok("Check-in thành công");
    }

    @PutMapping("/checkout/{bookingId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> checkOut(@PathVariable int bookingId) {
        try {
            bookingService.checkOutBooking(bookingId);
            return ResponseEntity.ok("Check-out thành công!");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Check-out thất bại: " + ex.getMessage());
        }
    }

    @PutMapping("/{bookingId}/apply-promotion")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> applyPromotion(
            @PathVariable int bookingId,
            @RequestParam String code
    ) {
        try {
            BookingResponseDTO result = bookingService.applyPromotion(bookingId, code);
            return ResponseEntity.ok(ApiResponse.success("Áp dụng mã giảm giá thành công", result));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/room/{roomId}/availability")
    public ResponseEntity<ApiResponse> getRoomAvailability(@PathVariable int roomId) {
        List<Map<String, String>> occupiedDates = bookingService.getRoomAvailability(roomId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch bận thành công", occupiedDates));
    }

    // ==========================================
    // 🔥 API THỐNG KÊ ADMIN (ĐẦY ĐỦ) 🔥
    // GET /api/v1/bookings/admin/dashboard-stats
    // ==========================================
    @GetMapping("/admin/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminDashboardStats() {
        try {
            // Gọi service lấy toàn bộ số liệu: Khách sạn, Phòng, Booking, Doanh thu, Review
            Map<String, Object> stats = bookingService.getAdminDashboardStats();
            return ResponseEntity.ok(ApiResponse.success("Lấy thống kê admin thành công", stats));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi lấy thống kê: " + ex.getMessage()));
        }
    }

    // ==========================================
    // API THỐNG KÊ OWNER (Nếu cần dùng sau này)
    // ==========================================
    @GetMapping("/owner/dashboard-stats")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> getOwnerDashboardStats(org.springframework.security.core.Authentication authentication) {
        try {
            com.example.smart_booking_system.security.CustomUserDetails userDetails = (com.example.smart_booking_system.security.CustomUserDetails) authentication.getPrincipal();
            String ownerId = userDetails.getUserId();
            Map<String, Object> stats = bookingService.getOwnerDashboardStats(ownerId);
            return ResponseEntity.ok(ApiResponse.success("Thành công", stats));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi: " + ex.getMessage()));
        }
    }
}