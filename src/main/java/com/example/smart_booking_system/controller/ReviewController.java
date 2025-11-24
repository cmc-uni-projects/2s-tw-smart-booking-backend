package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.dto.request.review.ReviewRequestDTO;
import com.example.smart_booking_system.dto.response.ApiResponse;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createReview(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ReviewRequestDTO request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Đánh giá thành công",
                    reviewService.createReview(currentUser.getUserId(), request)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/properties/{propertyId}")
    public ResponseEntity<?> getPropertyReviews(@PathVariable int propertyId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách đánh giá thành công",
                reviewService.getReviewsByProperty(propertyId)
        ));
    }
}