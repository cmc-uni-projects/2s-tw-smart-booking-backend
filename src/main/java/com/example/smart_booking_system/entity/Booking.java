package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId")
    private Room room; // always set: for villa/homestay it's the WHOLE room

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private Integer guestCount;  // Cho phép null

    private BigDecimal totalPrice;
    private BigDecimal penaltyAmount; // phạt (20%) nếu hủy muộn
    private BigDecimal refundAmount;  // tiền hoàn lại cho khách

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BookingStatus status = BookingStatus.CONFIRMED;

    private LocalDate createdAt = LocalDate.now();
}
