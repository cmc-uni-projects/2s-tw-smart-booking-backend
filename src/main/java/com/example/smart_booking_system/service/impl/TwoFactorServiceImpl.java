// src/main/java/com/example/smart_booking_system/service/impl/TwoFactorServiceImpl.java

package com.example.smart_booking_system.service.impl;

// ... Imports ...
import com.example.smart_booking_system.dto.request.auth.LoginRequest;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.exception.UnauthorizedException;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.repository.TwoFactorOtpRepository;
import com.example.smart_booking_system.entity.TwoFactorOtp;
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.TwoFactorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
// ... (và các imports khác như EmailService, UserRepository, PasswordEncoder)
// Giả định bạn đã inject các dependencies cần thiết

@Service
@RequiredArgsConstructor // Hoặc dùng @Autowired
@Transactional
public class TwoFactorServiceImpl implements TwoFactorService {

    private final TwoFactorOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder; // Cần để hash OTP và xác thực mật khẩu

    // Thời gian sống của OTP (5 phút)
    private static final int OTP_LIFETIME_MINUTES = 5;

    // --- LOGIC CHÍNH: TẠO VÀ GỬI OTP ---
    @Override
    public String generateAndSendOtp(User user) {
        // 1. Tạo OTP 6 chữ số ngẫu nhiên
        String otpCode = String.valueOf((int)(Math.random() * 900000) + 100000);
        String otpHashed = passwordEncoder.encode(otpCode);
        Instant expiryTime = Instant.now().plus(OTP_LIFETIME_MINUTES, ChronoUnit.MINUTES);

        // 2. Lưu OTP đã hash vào DB
        otpRepository.deleteByUser(user); // Xóa OTP cũ (nếu có)
        TwoFactorOtp twoFactorOtp = TwoFactorOtp.builder()
                .user(user)
                .otpHashed(otpHashed)
                .expirationTime(expiryTime)
                .build();
        otpRepository.save(twoFactorOtp);

        // 3. Tạo Session Token ngắn hạn
        String sessionToken = tokenProvider.generateTwoFactorSessionToken(user.getId());

        // 4. Gửi email (Sử dụng service đã có của bạn)
        emailService.send2FAEmail(user.getEmail(), otpCode, OTP_LIFETIME_MINUTES);

        return sessionToken;
    }

    // --- LOGIC: XÁC THỰC OTP ---
    @Override
    public boolean validateOtp(User user, String otpCode) {
        Optional<TwoFactorOtp> otpOpt = otpRepository.findByUserAndExpirationTimeAfter(user, Instant.now());

        if (otpOpt.isEmpty()) {
            throw new UnauthorizedException("OTP invalid or expired.");
        }

        TwoFactorOtp otpEntity = otpOpt.get();
        // So sánh OTP plaintext với OTP đã hash trong DB
        if (passwordEncoder.matches(otpCode, otpEntity.getOtpHashed())) {
            otpRepository.delete(otpEntity); // Xóa OTP sau khi dùng thành công
            return true;
        } else {
            throw new UnauthorizedException("Invalid OTP code.");
        }
    }

    // --- LOGIC: VALIDATE SESSION TOKEN ---
    @Override
    public User validateSessionToken(String token) {
        try {
            Long userId = tokenProvider.getUserIdFromTwoFactorSessionToken(token);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        } catch (Exception ex) {
            // Xử lý khi Token hết hạn hoặc không hợp lệ
            throw new UnauthorizedException("2FA Session Token expired or invalid.");
        }
    }

    // --- LOGIC: CÁC HÀM QUẢN LÝ 2FA ---
    @Override
    public void enable2FA(User user) {
        // Giả định hàm này chỉ được gọi sau khi đã xác thực OTP thành công
        user.setUsing2FA(true);
        userRepository.save(user);
    }

    @Override
    public void disable2FA(User user, LoginRequest credentials) {
        // Yêu cầu xác thực lại mật khẩu trước khi tắt
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid password for disabling 2FA.");
        }

        user.setUsing2FA(false);
        userRepository.save(user);
        cleanUpOtp(user);
    }

    // --- LOGIC: DỌN DẸP ---
    @Override
    public void cleanUpOtp(User user) {
        otpRepository.deleteByUser(user);
    }

    @Override
    public boolean is2FAEnabled(User user) {
        return user.isUsing2FA();
    }
}