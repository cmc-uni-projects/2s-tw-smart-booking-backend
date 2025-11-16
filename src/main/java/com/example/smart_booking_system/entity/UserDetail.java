package com.example.smart_booking_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate; // <-- THÊM IMPORT NÀY
import java.time.LocalDateTime;

@Entity
@Table(name = "userdetail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userdetailId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(length = 50)
    private String gender;

    // === BẮT ĐẦU CODE MỚI ===
    @Column
    private LocalDate dateOfBirth; // <-- THÊM TRƯỜNG NÀY
    // === KẾT THÚC CODE MỚI ===

    @Column(length = 512)
    private String profilePhotoUrl;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}