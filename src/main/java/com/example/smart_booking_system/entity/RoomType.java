package com.example.smart_booking_system.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
public class RoomType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int roomTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertId")
    private Property propertyId;

    private String roomTypeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal pricePerNight;

    private int capacity;

    @Column(columnDefinition = "TEXT")
    private String policy;

}
