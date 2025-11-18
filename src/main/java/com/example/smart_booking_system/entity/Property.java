package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int propertyId;

    private String propertyName;

    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User owner;

    private String address;
    private String country;
    private String city;

    private String province;

    private String postalCode;

    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    private String phoneContact;
    private String emailContact;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    private int reviewCount = 0;

    private boolean isActive = false;
    @Enumerated(EnumType.STRING)
    private PropertyStatus propertyStatus;
    private LocalDate createdAt = LocalDate.now();
    private LocalDate updatedAt = LocalDate.now();
    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<PropertyAmenity> propertyAmenities;
    @OneToMany(mappedBy = "propertyId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Room> rooms;
}