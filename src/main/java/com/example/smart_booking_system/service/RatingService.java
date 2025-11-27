package com.example.smart_booking_system.service;


import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.RatingType;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.RatingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;

import java.util.List;

@Service
@AllArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;

    private static final int PAGE_SIZE = 10;

    private RatingType classify(int stars, String comment) {
        if (comment == null) comment = "";
        String lower = comment.toLowerCase();

        // Vi phạm
        if (lower.contains("dm") || lower.contains("địt") || lower.contains("cút")
                || lower.contains("fuck") || lower.contains("shit")
                || lower.contains("bố mày") || lower.contains("óc chó")
                || lower.contains("ngu") ) {
            return RatingType.VIOLATION;
        }

        // Tích cực
        if (stars >= 4 ||
                lower.contains("good") ||
                lower.contains("tốt") ||
                lower.contains("great") ||
                lower.contains("tuyệt") ||
                lower.contains("hài lòng")) {
            return RatingType.POSITIVE;
        }

        // Tiêu cực
        if (stars <= 2 ||
                lower.contains("bad") ||
                lower.contains("tệ") ||
                lower.contains("kém") ||
                lower.contains("không hài lòng")) {
            return RatingType.NEGATIVE;
        }


        return RatingType.NEGATIVE;
    }

    public Rating createRating(Rating rating){
        Booking booking = bookingRepository.findById(
                rating.getBookingId().getBookingId()
        ).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Booking must be COMPLETE before rating.");
        }
        RatingType type = classify(rating.getRating(), rating.getComment());

        if (type == RatingType.VIOLATION) {
            throw new RuntimeException("Rating contains abusive content and cannot be saved.");
        }

        rating.setRatingType(type);
        rating.setHidden(false);

        return ratingRepository.save(rating);

    }

    public Rating getRatingById(int id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        if (rating.isHidden()) {
            throw new RuntimeException("This rating is hidden.");
        }

        return rating;
    }

    private Page<Rating> paginate(List<Rating> list, int page) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, list.size());
        List<Rating> content = list.subList(start, end);
        return new PageImpl<>(content, PageRequest.of(page, PAGE_SIZE), list.size());
    }

    public Page<Rating> getRatingByBooking(int bookingId, int page) {
        List<Rating> all = ratingRepository.getRatingByBookingId(bookingId);
        return paginate(all, page);
    }

    public Page<Rating> getRatingByUser(String userId, int page) {
        List<Rating> all = ratingRepository.getRatingByUserId(userId);
        return paginate(all, page);
    }

    public Page<Rating> getRatingByType(String ratingType, int page) {
        List<Rating> all = ratingRepository.getRatingByRatingType(ratingType);
        return paginate(all, page);
    }

    public Page<Rating> getHiddenRatings(int page) {
        List<Rating> all = ratingRepository.getRatingByHidden();
        return paginate(all, page);
    }

    public Rating updateRating(int id, Rating updated) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setRating(updated.getRating());
        rating.setComment(updated.getComment());

        RatingType type = classify(updated.getRating(), updated.getComment());

        if (type == RatingType.VIOLATION) {
            throw new RuntimeException("Updated rating contains abusive content and cannot be saved.");
        }

        rating.setRatingType(type);

        return ratingRepository.save(rating);
    }

    public Rating hideRating(int ratingId, boolean hide) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setHidden(hide);
        return ratingRepository.save(rating);
    }

    public void deleteRating(int id) {
        if (!ratingRepository.existsById(id)) {
            throw new RuntimeException("Rating not found");
        }
        ratingRepository.deleteById(id);
    }

    public Page<Rating> getRatingsForProperty(int propertyId, int page) {

        List<Rating> all = ratingRepository.getRatingsByProperty(propertyId);

        List<Rating> good = all.stream()
                .filter(r -> r.getRatingType() == RatingType.POSITIVE)
                .toList();

        List<Rating> bad = all.stream()
                .filter(r -> r.getRatingType() == RatingType.NEGATIVE)
                .toList();

        int goodCount = (int) Math.ceil(all.size() * 0.8);
        int badCount = (int) Math.ceil(all.size() * 0.2);

        goodCount = Math.min(goodCount, good.size());
        badCount = Math.min(badCount, bad.size());

        List<Rating> mixed = new ArrayList<>();
        mixed.addAll(good.subList(0, goodCount));
        mixed.addAll(bad.subList(0, badCount));

        Collections.shuffle(mixed);

        int pageSize = 10;
        int start = page * pageSize;
        int end = Math.min(start + pageSize, mixed.size());

        List<Rating> pageContent = mixed.subList(start, end);

        return new PageImpl<>(pageContent, PageRequest.of(page, pageSize), mixed.size());


    }
}
