package com.example.smart_booking_system.service.impl;

import com.example.smart_booking_system.dto.request.auth.*;
import com.example.smart_booking_system.dto.response.auth.LoginResponse;
import com.example.smart_booking_system.entity.Role;
import com.example.smart_booking_system.entity.User;
import com.example.smart_booking_system.exception.*;
import com.example.smart_booking_system.repository.RoleRepository;
import com.example.smart_booking_system.repository.UserRepository;
import com.example.smart_booking_system.security.CustomUserDetails;
import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.service.AuthService;
import com.example.smart_booking_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // ✅ Đăng ký người dùng mới
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.isPasswordMatching()) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("INACTIVE");
        user.setIsEmailVerified(false);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(10));

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'CUSTOMER' not found"));
        user.addRole(customerRole);

        userRepository.save(user);

        // Gửi email xác minh
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);
    }

    // ✅ Đăng nhập
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Sai email hoặc mật khẩu"));

        if (Boolean.FALSE.equals(user.getIsEmailVerified())) {
            throw new UnauthorizedException("Hãy xác minh email của bạn trước khi đăng nhập");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus()) || "BANNED".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị " + user.getStatus().toLowerCase());
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        long expiresIn = tokenProvider.getExpirationTime();

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<String> roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        LoginResponse.UserResponse userResponse = new LoginResponse.UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getIsEmailVerified(),
                user.getStatus(),
                roles
        );

        return new LoginResponse(token, expiresIn, userResponse);
    }

    // ✅ Xác minh email
    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        user.setIsEmailVerified(true);
        user.setStatus("ACTIVE");
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ✅ Quên mật khẩu
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), resetToken);
    }

    // ✅ Đặt lại mật khẩu
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ✅ Đổi mật khẩu
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Old password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ✅ Gửi lại email xác minh
    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new BadRequestException("Email is already verified");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);
    }
}
