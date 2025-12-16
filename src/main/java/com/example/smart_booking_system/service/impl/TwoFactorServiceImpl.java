package com.example.smart_booking_system.service.impl;

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
import java.util.Optional; // FIX: Thêm import bị thiếu

@Service
@RequiredArgsConstructor
@Transactional
public class TwoFactorServiceImpl implements TwoFactorService {

    private final TwoFactorOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_LIFETIME_MINUTES = 5;

    // --- LOGIC CHÍNH: TẠO VÀ GỬI OTP ---

    @Override
    public String generateAndSendOtp(User user) {
        // 1. Tạo OTP 6 chữ số ngẫu nhiên (KHÔI PHỤC CODE BỊ THIẾU)
        String otpCode = String.valueOf((int)(Math.random() * 900000) + 100000);
        String otpHashed = passwordEncoder.encode(otpCode);
        Instant expiryTime = Instant.now().plus(OTP_LIFETIME_MINUTES, ChronoUnit.MINUTES);

        // 2. Lưu OTP đã hash vào DB
        otpRepository.deleteByUser(user);
        TwoFactorOtp twoFactorOtp = TwoFactorOtp.builder()
                .user(user)
                .otpHashed(otpHashed)
                .expirationTime(expiryTime)
                .build();
        otpRepository.save(twoFactorOtp);

        // 3. Tạo Session Token ngắn hạn
        // Lỗi này phụ thuộc vào JwtTokenProvider (xem bên dưới)
        String sessionToken = tokenProvider.generateTwoFactorSessionToken(user.getUserId());

        // 4. Gửi email
        emailService.send2FAEmail(user.getEmail(), user.getFullName(), otpCode, OTP_LIFETIME_MINUTES);

        return sessionToken;
    }
    // --- LOGIC: XÁC THỰC OTP ---
    @Override
    public boolean validateOtp(User user, String otpCode) {
        // FIX: Refactor logic để loại bỏ các lỗi Optional.isEmpty() và .get()
        TwoFactorOtp otpEntity = otpRepository.findByUserAndExpirationTimeAfter(user, Instant.now())
                .orElseThrow(() -> new UnauthorizedException("Mã OTP không hợp lệ hoặc đã hết hạn."));

        // So sánh OTP plaintext với OTP đã hash trong DB
        if (passwordEncoder.matches(otpCode, otpEntity.getOtpHashed())) {
            otpRepository.delete(otpEntity); // Xóa OTP sau khi dùng thành công
            return true;
        } else {
            throw new UnauthorizedException("Mã OTP không chính xác.");
        }
    }

    // --- LOGIC: VALIDATE SESSION TOKEN ---
    @Override
    public User validateSessionToken(String token) {
        try {
            // Nhận về String
            String userId = tokenProvider.getUserIdFromTwoFactorSessionToken(token);

            // UserRepository.findById(String)
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tìm thấy với ID: " + userId));

        } catch (Exception ex) {
            // Xử lý khi Token hết hạn hoặc không hợp lệ
            throw new UnauthorizedException("Phiên xác thực 2FA đã hết hạn hoặc không hợp lệ.");
        }
    }

    // --- LOGIC: CÁC HÀM QUẢN LÝ 2FA ---
    @Override
    public void enable2FA(User user) {
        // ... (Logic giữ nguyên)
        user.setUsing2FA(true);
        userRepository.save(user);
    }

    @Override
    public void disable2FA(User user, LoginRequest credentials) {
        // ... (Logic giữ nguyên)
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPasswordHash())) {
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