package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.request.auth.LoginRequest;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.ResourceNotFoundException;
import com.example.smart_booking_system.exception.UnauthorizedException;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.JwtTokenProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation; // ✅ Cần thiết
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.repository.TwoFactorOtpRepository;
import com.example.smart_booking_system.entity.TwoFactorOtp;
// import jakarta.transaction.Transactional; // 🛑 Bỏ hoặc loại bỏ vì dùng Spring @Transactional
import org.springframework.transaction.annotation.Transactional; // ✅ Dùng Spring Annotation
import com.example.smart_booking_system.service.EmailService;
import com.example.smart_booking_system.service.TwoFactorService;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.smart_booking_system.exception.BadRequestException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional // Spring @Transactional ở cấp class
public class TwoFactorServiceImpl implements TwoFactorService {

    private final TwoFactorOtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_LIFETIME_MINUTES = 5;

    // --- LOGIC CHÍNH: TẠO VÀ GỬI OTP (FIX REQUIRES_NEW) ---

    @Override
    // 🛑 FIX: Yêu cầu một Transaction MỚI và độc lập để đảm bảo việc lưu (commit)
    @Transactional(propagation = Propagation.REQUIRES_NEW) // ✅ FIX SỬ DỤNG ĐÚNG ANNOTATION
    public String generateAndSendOtp(User user) {

        // 1. Tạo OTP và tính thời gian hết hạn
        String otpCode = String.valueOf((int)(Math.random() * 900000) + 100000);
        String otpHashed = passwordEncoder.encode(otpCode);
        Instant expiryTime = Instant.now().plus(OTP_LIFETIME_MINUTES, ChronoUnit.MINUTES);

        // 2. TÌM VÀ CẬP NHẬT/TẠO MỚI (Dùng findByUserId an toàn)
        Optional<TwoFactorOtp> existingOtp = otpRepository.findByUser_UserId(user.getUserId());

        if (existingOtp.isPresent()) {
            TwoFactorOtp otpToUpdate = existingOtp.get();
            otpToUpdate.setOtpHashed(otpHashed);
            otpToUpdate.setExpirationTime(expiryTime);
            otpRepository.save(otpToUpdate);
        } else {
            TwoFactorOtp newOtp = TwoFactorOtp.builder()
                    .user(user)
                    .otpHashed(otpHashed)
                    .expirationTime(expiryTime)
                    .build();
            otpRepository.save(newOtp);
        }

        // Bỏ otpRepository.flush() vì REQUIRES_NEW sẽ commit tự động

        // 3. Tạo Session Token ngắn hạn
        String sessionToken = tokenProvider.generateTwoFactorSessionToken(user.getUserId());

        // 4. Gửi email
        emailService.send2FAEmail(user.getEmail(), user.getFullName(), otpCode, OTP_LIFETIME_MINUTES);

        return sessionToken;
    }

    // --- LOGIC: XÁC THỰC OTP (FIX LỖI KHÔNG TÌM THẤY OTP) ---
    @Override
    @Transactional // Giữ nguyên Transaction Isolation Level của class
    public boolean validateOtp(User user, String otpCode) {

        // 1. Tìm bản ghi OTP hiện có của User
        TwoFactorOtp otpEntity = otpRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP. Vui lòng gửi lại mã mới."));

        // 2. Kiểm tra thời gian hết hạn thủ công bằng Java
        if (Instant.now().isAfter(otpEntity.getExpirationTime())) {
            otpRepository.delete(otpEntity);
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        }

        // 3. ✅ SỬA LỖI: Dùng .trim() để loại bỏ khoảng trắng (thường là nguyên nhân chính)
        String trimmedOtpCode = otpCode.trim();

        if (passwordEncoder.matches(trimmedOtpCode, otpEntity.getOtpHashed())) {
            // Xác thực thành công
            otpRepository.delete(otpEntity);
            return true;
        } else {
            // Mã không chính xác
            throw new BadRequestException("Mã OTP không chính xác.");
        }
    }

    // --- LOGIC CÒN LẠI (Giữ nguyên) ---
    @Override
    @Transactional // Giữ nguyên
    public User validateSessionToken(String token) {
        try {
            String userId = tokenProvider.getUserIdFromTwoFactorSessionToken(token);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tìm thấy với ID: " + userId));

        } catch (Exception ex) {
            throw new UnauthorizedException("Phiên xác thực 2FA đã hết hạn hoặc không hợp lệ.");
        }
    }

    @Override
    @Transactional // Giữ nguyên
    public void enable2FA(User user) {
        user.setUsing2FA(true);
        userRepository.save(user);

        // ✅ FIX LỖI CACHE JWT/CONTEXT: Cập nhật Security Context
        // Việc này đảm bảo các API tiếp theo (như /status) sẽ đọc trạng thái mới nhất

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Chỉ cập nhật nếu user đã đăng nhập (Authentication object tồn tại)
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

            // 1. Cập nhật trạng thái 2FA trong đối tượng CustomUserDetails đang giữ trong Context
            customUserDetails.setUsing2FA(true);

            // 2. Tạo lại Authentication Token và đặt vào Context
            Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                    customUserDetails,
                    authentication.getCredentials(),
                    customUserDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuthentication);
        }
    }

    @Override
    @Transactional // Giữ nguyên
    public void disable2FA(User user, LoginRequest credentials) {
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid password for disabling 2FA.");
        }

        user.setUsing2FA(false);
        userRepository.save(user);
        cleanUpOtp(user);
    }

    @Override
    @Transactional
    public void cleanUpOtp(User user) {
        otpRepository.deleteByUser(user);
    }

    @Override
    @Transactional // Đảm bảo thực thi trong một transaction
    public boolean is2FAEnabled(User user) {
        return userRepository.findById(user.getUserId())
                .map(User::isUsing2FA) // Nếu tìm thấy, lấy trạng thái isUsing2FA
                .orElse(user.isUsing2FA()); // Nếu không tìm thấy (cực hiếm), dùng trạng thái cũ
    }
    public JwtTokenProvider getJwtTokenProvider() {
        return this.tokenProvider;
    }
}