package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.RoomCategory;
import com.example.smart_booking_system.enums.RoomStatus;
import com.mysql.cj.protocol.ColumnDefinition;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int roomId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propertyId")
    private Property propertyId;
    @Enumerated(EnumType.STRING)
    private RoomCategory roomCategory;
    private String roomName;
    @Column(columnDefinition = "TEXT")
    private String description;
    private int capacity;
    private BigDecimal pricePerNight;
    private BigDecimal weekendPrice;
    @Enumerated(EnumType.STRING)
    private RoomStatus roomStatus;
    private boolean isActive = true;
}
