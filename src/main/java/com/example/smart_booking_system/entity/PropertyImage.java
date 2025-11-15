package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "propertyimage")
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int propertyImageId;   // <-- FIELD NÀY PHẢI CÓ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId")
    private Property property;

    private String imageUrl;

    private boolean isActive = true;
}
