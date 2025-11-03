package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ownerApplications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerApplication {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "applicationId", updatable = false, nullable = false)
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(name = "businessName", nullable = false)
    private String businessName;

    @Column(name = "businessAddress", length = 500)
    private String businessAddress;

    @Column(name = "businessLicense")
    private String businessLicense;

    @Column(name = "businessLicenseUrl")
    private String businessLicenseUrl;

    @Column(name = "identityCardUrl")
    private String identityCardUrl;

    @Column(name = "phoneNumber", length = 32)
    private String phoneNumber;

    @Column(name = "taxCode", length = 50)
    private String taxCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 32)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejectionReason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewedBy")
    private User reviewedBy;

    @Column(name = "reviewedAt")
    private LocalDateTime reviewedAt;

    @Column(name = "createdAt", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}