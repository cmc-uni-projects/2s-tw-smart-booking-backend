package com.example.smart_booking_system.entity;


import com.example.smart_booking_system.enums.RatingType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Rating")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int ratingId;

    @JoinColumn(name = "users")
    @ManyToOne
    private User userId;

    @JoinColumn(name = "bookings")
    @ManyToOne
    private Booking bookingId;

    @Column(precision = 2, scale = 1)
    private int rating;

    @Column(nullable = true)
    private String comment;

    private RatingType ratingType;

    @Column(name = "isHidden")
    private boolean isHidden;

}
