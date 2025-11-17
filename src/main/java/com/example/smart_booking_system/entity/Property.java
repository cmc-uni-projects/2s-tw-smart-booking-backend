package com.example.smart_booking_system.entity;

import com.example.smart_booking_system.enums.PropertyStatus;
import com.example.smart_booking_system.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int propertyId;

    // Tên cơ sở
    private String propertyName;

    // Loại hình (HOTEL, VILLA,...)
    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;

    // ✅ SỬA ĐỔI 1: Đổi tên 'OwnerId' thành 'owner' cho chuẩn convention
    // Service sẽ gọi: property.setOwner(user);
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId") // Tên cột trong DB vẫn là userId
    private User owner;

    // --- ĐỊA CHỈ ---
    private String address;
    private String country;
    private String city;

    // ✅ SỬA ĐỔI 2: Thêm trường Province (Tỉnh/Thành) khớp với Frontend
    private String province;

    private String postalCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ❌ ĐÃ XÓA: amenitiesJson và imageUrlsJson
    // (Vì dữ liệu này giờ đã được lưu sang bảng PropertyAmenity và PropertyImage)

    // --- TỌA ĐỘ (Có thể để mặc định 0 nếu chưa dùng map) ---
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    // --- LIÊN HỆ ---
    private String phoneContact;
    private String emailContact;

    // --- ĐÁNH GIÁ ---
    @Column(precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    private int reviewCount = 0;

    // --- TRẠNG THÁI ---
    private boolean isActive = false;

    @Enumerated(EnumType.STRING)
    private PropertyStatus propertyStatus;

    // --- TIMESTAMP ---
    private LocalDate createdAt = LocalDate.now();

    private LocalDate updatedAt = LocalDate.now();
}