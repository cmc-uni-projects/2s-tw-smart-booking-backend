package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.BookingResponseDTO;
import com.example.smart_booking_system.dto.request.BookingRequestDTO;
import com.example.smart_booking_system.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

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
            return ResponseEntity.badRequest().body("Create booking failed: " + ex.getMessage());
        }
    }

    // ==========================================
    // CANCEL BOOKING
    // ==========================================
    @PutMapping("/cancel/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable int bookingId) {
        try {
            BookingResponseDTO result = bookingService.cancelBooking(bookingId);
            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Cancel booking failed: " + ex.getMessage());
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
            return ResponseEntity.badRequest().body("Get booking failed: " + ex.getMessage());
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
            return ResponseEntity.badRequest().body("Get bookings by user failed: " + ex.getMessage());
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
            return ResponseEntity.badRequest().body("Get bookings by property failed: " + ex.getMessage());
        }
    }

    // ==========================================
    // GET ALL BOOKINGS (ADMIN)
    // ==========================================
    @GetMapping("")
    public ResponseEntity<?> getAllBookings() {
        try {
            return ResponseEntity.ok(bookingService.getAllBookings());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Get all bookings failed: " + ex.getMessage());
        }
    }
}
