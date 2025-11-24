package com.example.smart_booking_system.service;

import com.example.smart_booking_system.dto.request.review.ReviewRequestDTO;
import com.example.smart_booking_system.dto.response.review.ReviewResponseDTO;
import com.example.smart_booking_system.entity.Booking;
import com.example.smart_booking_system.entity.Property;
import com.example.smart_booking_system.entity.Review;
import com.example.smart_booking_system.enums.BookingStatus;
import com.example.smart_booking_system.exception.BadRequestException;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.repository.BookingRepository;
import com.example.smart_booking_system.repository.PropertyRepository;
import com.example.smart_booking_system.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    public ReviewResponseDTO createReview(String userId, ReviewRequestDTO request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn đặt phòng"));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền đánh giá đơn này");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Đơn đặt phòng chưa hoàn thành hoặc đã bị hủy");
        }

        LocalDateTime checkOutTime = booking.getCheckOutDate().atTime(LocalTime.of(12, 0));
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(checkOutTime)) {
            throw new BadRequestException("Bạn chưa trả phòng (Check-out), chưa thể đánh giá lúc này.");
        }

        if (reviewRepository.existsByBooking_BookingId(request.getBookingId())) {
            throw new BadRequestException("Bạn đã đánh giá đơn đặt phòng này rồi");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        updatePropertyRating(booking.getProperty());

        return new ReviewResponseDTO(savedReview);
    }

    public List<ReviewResponseDTO> getReviewsByProperty(int propertyId) {
        return reviewRepository.findAllByPropertyId(propertyId).stream()
                .map(ReviewResponseDTO::new)
                .collect(Collectors.toList());
    }

    private void updatePropertyRating(Property property) {
        List<Review> reviews = reviewRepository.findAllByPropertyId(property.getPropertyId());

        if (reviews.isEmpty()) return;

        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        property.setRating(BigDecimal.valueOf(average));
        property.setReviewCount(reviews.size());
        propertyRepository.save(property);
    }
}