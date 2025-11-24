package com.example.smart_booking_system.dto.request.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequestDTO {

    @NotNull(message = "Booking ID không được để trống")
    private Integer bookingId;

    @NotNull(message = "Điểm đánh giá là bắt buộc")
    @Min(value = 1, message = "Điểm thấp nhất là 1")
    @Max(value = 5, message = "Điểm cao nhất là 5")
    private Integer rating;

    private String comment;
}