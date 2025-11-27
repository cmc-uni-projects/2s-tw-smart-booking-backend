package com.example.smart_booking_system.controller;

import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // ===================== CREATE =====================
    @PostMapping("/create")
    public Rating createRating(@RequestBody Rating rating) {
        return ratingService.createRating(rating);
    }

    // ===================== GET ONE =====================
    @GetMapping("/{id}")
    public Rating getRatingById(@PathVariable int id) {
        return ratingService.getRatingById(id);
    }

    // ===================== GET BY BOOKING =====================
    @GetMapping("/booking/{bookingId}")
    public Page<Rating> getByBooking(
            @PathVariable int bookingId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ratingService.getRatingByBooking(bookingId, page);
    }

    // ===================== GET BY USER =====================
    @GetMapping("/user/{userId}")
    public Page<Rating> getByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ratingService.getRatingByUser(userId, page);
    }

    // ===================== GET BY TYPE =====================
    @GetMapping("/type/{type}")
    public Page<Rating> getByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ratingService.getRatingByType(type, page);
    }

    // ===================== GET HIDDEN (ADMIN ONLY) =====================
    @GetMapping("/hidden")
    public Page<Rating> getHiddenRatings(
            @RequestParam(defaultValue = "0") int page
    ) {
        return ratingService.getHiddenRatings(page);
    }

    // ===================== GET BY PROPERTY =====================
    @GetMapping("/property/{propertyId}")
    public Page<Rating> getByProperty(
            @PathVariable int propertyId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ratingService.getRatingsForProperty(propertyId, page);
    }

    // ===================== UPDATE =====================
    @PutMapping("/update/{id}")
    public Rating updateRating(
            @PathVariable int id,
            @RequestBody Rating rating
    ) {
        return ratingService.updateRating(id, rating);
    }

    // ===================== HIDE / UNHIDE =====================
    @PatchMapping("/hide/{id}")
    public Rating hideRating(
            @PathVariable int id,
            @RequestParam boolean hide
    ) {
        return ratingService.hideRating(id, hide);
    }

    // ===================== DELETE =====================
    @DeleteMapping("/delete/{id}")
    public void deleteRating(@PathVariable int id) {
        ratingService.deleteRating(id);
    }
}
