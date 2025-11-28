package com.example.smart_booking_system.service;

import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Rating;
import com.example.smart_booking_system.entity.RatingImage;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.enums.RatingType;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.RatingImageRepository;
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
    private final RatingImageRepository ratingImageRepository;

    private static final int PAGE_SIZE = 10;

    private Rating attachImages(Rating rating) {
        List<RatingImage> imgs =
                ratingImageRepository.getImagesByRatingId(rating.getRatingId());
        rating.setImages(imgs);
        return rating;
    }

    private List<Rating> attachImages(List<Rating> list) {
        list.forEach(r ->
                r.setImages(ratingImageRepository.getImagesByRatingId(r.getRatingId()))
        );
        return list;
    }

    private RatingType classify(int stars, String comment) {
        if (comment == null) comment = "";
        String lower = comment.toLowerCase();

        if (lower.contains("dm") || lower.contains("địt") || lower.contains("cút")
                || lower.contains("fuck") || lower.contains("shit")
                || lower.contains("bố mày") || lower.contains("óc chó")
                || lower.contains("ngu")|| lower.contains("lol")) {
            return RatingType.VIOLATION;
        }

        if (stars >= 4 ||
                lower.contains("good") ||
                lower.contains("tốt") ||
                lower.contains("great") ||
                lower.contains("tuyệt") ||
                lower.contains("hài lòng")) {
            return RatingType.POSITIVE;
        }

        if (stars <= 2 ||
                lower.contains("bad") ||
                lower.contains("tệ") ||
                lower.contains("kém") ||
                lower.contains("không hài lòng")) {
            return RatingType.NEGATIVE;
        }

        return RatingType.NEGATIVE;
    }

    public Rating createRating(Rating rating) {

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

        Rating saved = ratingRepository.save(rating);

        if (rating.getImages() != null) {
            for (RatingImage img : rating.getImages()) {
                img.setRating(saved);
                ratingImageRepository.save(img);
            }
        }

        return attachImages(saved);
    }

    public Rating getRatingById(int id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        if (rating.isHidden()) {
            throw new RuntimeException("This rating is hidden.");
        }

        return attachImages(rating);
    }


    private Page<Rating> paginate(List<Rating> list, int page) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, list.size());
        List<Rating> content = list.subList(start, end);
        attachImages(content);
        return new PageImpl<>(content, PageRequest.of(page, PAGE_SIZE), list.size());
    }

    public Page<Rating> getRatingByBooking(int bookingId, int page) {
        return paginate(ratingRepository.getRatingByBookingId(bookingId), page);
    }

    public Page<Rating> getRatingByUser(String userId, int page) {
        return paginate(ratingRepository.getRatingByUserId(userId), page);
    }

    public Page<Rating> getRatingByType(String ratingType, int page) {
        return paginate(ratingRepository.getRatingByRatingType(ratingType), page);
    }

    public Page<Rating> getHiddenRatings(int page) {
        return paginate(ratingRepository.getRatingByHidden(), page);
    }

    public Page<Rating> getRatingsForProperty(int propertyId, int page) {

        List<Rating> all = ratingRepository.getRatingsByProperty(propertyId);

        List<Rating> good = all.stream()
                .filter(r -> r.getRatingType() == RatingType.POSITIVE)
                .toList();

        List<Rating> bad = all.stream()
                .filter(r -> r.getRatingType() == RatingType.NEGATIVE)
                .toList();

        int goodCount = Math.min((int) Math.ceil(all.size() * 0.8), good.size());
        int badCount = Math.min((int) Math.ceil(all.size() * 0.2), bad.size());

        List<Rating> mixed = new ArrayList<>();
        mixed.addAll(good.subList(0, goodCount));
        mixed.addAll(bad.subList(0, badCount));

        Collections.shuffle(mixed);

        return paginate(mixed, page);
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

        // Xóa toàn bộ ảnh cũ
        List<RatingImage> oldImages = ratingImageRepository.getImagesByRatingId(id);
        for (RatingImage img : oldImages) {
            ratingImageRepository.delete(img);
        }

        // Lưu ảnh mới
        if (updated.getImages() != null) {
            for (RatingImage img : updated.getImages()) {
                img.setRating(rating);
                ratingImageRepository.save(img);
            }
        }

        return attachImages(ratingRepository.save(rating));
    }

    public Rating hideRating(int ratingId, boolean hide) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setHidden(hide);
        return attachImages(ratingRepository.save(rating));
    }

    public void deleteRating(int id) {
        List<RatingImage> imgs = ratingImageRepository.getImagesByRatingId(id);
        imgs.forEach(ratingImageRepository::delete);

        if (!ratingRepository.existsById(id)) {
            throw new RuntimeException("Rating not found");
        }

        ratingRepository.deleteById(id);
    }
}
