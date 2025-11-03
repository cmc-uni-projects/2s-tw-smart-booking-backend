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
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setStatus("INACTIVE");
        user.setIsEmailVerified(false);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.addRole(customerRole);

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.getIsEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in");
        }

        if ("SUSPENDED".equals(user.getStatus()) || "BANNED".equals(user.getStatus())) {
            throw new UnauthorizedException("Your account is not active");
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
                user.getUserId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getIsEmailVerified(),
                user.getStatus(),
                roles
        );

        return new LoginResponse(token, expiresIn, userResponse);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        user.setIsEmailVerified(true);
        user.setStatus("ACTIVE");
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), user.getFullName(), resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

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
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationToken);
    }
}
