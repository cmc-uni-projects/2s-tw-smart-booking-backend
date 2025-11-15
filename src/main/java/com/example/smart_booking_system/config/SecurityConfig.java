package com.example.smart_booking_system.config;

import com.example.smart_booking_system.security.CustomUserDetailsService;
import com.example.smart_booking_system.security.JwtAuthenticationEntryPoint;
import com.example.smart_booking_system.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Thêm import này
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Giữ lại EnableMethodSecurity để @PreAuthorize vẫn hoạt động
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Cấu hình CORS và CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // Xử lý lỗi xác thực
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))

                // Quản lý session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Cấu hình các quy tắc bảo vệ
                .authorizeHttpRequests(authorize -> authorize
                        // ===== 1. PUBLIC ROUTES (Phải được định nghĩa đầu tiên) =====
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/uploads/**", "/images/**", "/api/v1/files/**").permitAll()
                        .requestMatchers("/api/v1/properties/search").permitAll()
                        .requestMatchers("/api/v1/properties/featured").permitAll()
                        .requestMatchers("/api/v1/properties/{id}").permitAll()

                        // ===== 2. ADMIN ROUTES =====
                        // ✅ ĐÃ CHUYỂN SANG hasRole
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ===== 3. OWNER ROUTES =====
                        // ✅ ĐÃ CHUYỂN SANG hasRole
                        .requestMatchers("/api/v1/properties/submit-application").hasRole("OWNER")
                        .requestMatchers("/api/v1/owner/**").hasRole("OWNER")

                        // ===== 4. CUSTOMER ROUTES =====
                        // ✅ ĐÃ CHUYỂN SANG hasRole
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/applications/owner/**").hasRole("CUSTOMER")

                        // ===== 5. MIXED/COMBO ROUTES (Owner & Admin) =====
                        // ✅ ĐÃ CHUYỂN SANG hasAnyRole
                        .requestMatchers("/api/v1/properties/add").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/properties/update/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/room/**").hasAnyRole("OWNER", "ADMIN")

                        // ===== 6. DEFAULT =====
                        .anyRequest().authenticated()
                );

        // Thêm các provider và filter
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}