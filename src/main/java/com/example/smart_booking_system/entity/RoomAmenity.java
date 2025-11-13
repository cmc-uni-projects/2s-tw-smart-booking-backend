package com.example.smart_booking_system.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "RoomAmentity")
public class RoomAmenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int roomAmenityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenityId")
    private Amenity amenityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId")
    private Room roomId;

}
