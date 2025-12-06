package com.example.smart_booking_system.security;

import com.example.smart_booking_system.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User; // <--- Import mới

import java.util.Collection;
import java.util.Map; // <--- Import mới
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User { // <--- Implements thêm OAuth2User

    private String userId;
    private String email;
    private String password;
    private String fullName;
    private Boolean isEmailVerified;
    private String status;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes; // <--- Thêm trường để lưu attributes từ Google/FB

    /**
     * Create UserDetails from User entity (Dùng cho Login thường)
     */
    public static CustomUserDetails create(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toSet());

        return new CustomUserDetails(
                user.getUserId().toString(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getIsEmailVerified(),
                user.getStatus(),
                authorities,
                null // Attributes là null khi login thường
        );
    }

    /**
     * Create UserDetails from User entity & Attributes (Dùng cho OAuth2)
     */
    public static CustomUserDetails create(User user, Map<String, Object> attributes) {
        CustomUserDetails userDetails = CustomUserDetails.create(user);
        userDetails.setAttributes(attributes);
        return userDetails;
    }

    // --- UserDetails Methods ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equals(status) && !"BANNED".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }

    // --- OAuth2User Methods (Mới thêm) ---
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(userId); // Trả về userId làm định danh chính
    }
}