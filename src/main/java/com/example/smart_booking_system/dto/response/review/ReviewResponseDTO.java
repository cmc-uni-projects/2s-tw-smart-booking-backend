package com.example.smart_booking_system.dto.response.review;

import com.example.smart_booking_system.entity.Review;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReviewResponseDTO {
    private int reviewId;
    private int bookingId;
    private String customerName;
    private String customerAvatar;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    public ReviewResponseDTO(Review review) {
        this.reviewId = review.getReviewId();
        this.bookingId = review.getBooking().getBookingId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.createdAt = review.getCreatedAt();

        if (review.getBooking().getUser() != null) {
            this.customerName = review.getBooking().getUser().getFullName();
            if (review.getBooking().getUser().getUserDetail() != null) {
                this.customerAvatar = review.getBooking().getUser().getUserDetail().getProfilePhotoUrl();
            }
        }
    }
}