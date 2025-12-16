// src/main/java/com/example/smart_booking_system/entity/TwoFactorOtp.java

package com.example.smart_booking_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "twoFactorOtp")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết 1-1 với User
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    // MÃ OTP ĐÃ BĂM (KHÔNG BAO GIỜ LƯU PLAINTEXT)
    @Column(name = "otpHashed", nullable = false)
    private String otpHashed;

    @Column(name = "expirationTime", nullable = false)
    private Instant expirationTime;
}