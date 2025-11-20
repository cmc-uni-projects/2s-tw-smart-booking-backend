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
@EnableMethodSecurity
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // ===== 1. PUBLIC ROUTES (Phải được định nghĩa đầu tiên) =====
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/uploads/**", "/images/**", "/properties/**", "/api/v1/files/**").permitAll()
                        .requestMatchers("/api/v1/properties/search").permitAll()
                        .requestMatchers("/api/v1/properties/featured").permitAll()
                        .requestMatchers("/api/v1/properties/{id}").permitAll()

                        // === BẮT ĐẦU SỬA LỖI ===
                        // ===== 2. GENERAL AUTHENTICATED ROUTES =====
                        // (Thêm các route này, vì nó không thuộc /admin, /owner, hay /customer)
                        .requestMatchers(
                                "/api/v1/user-details/me",
                                "/api/v1/user-details/update",
                                "/api/v1/user-details/profile-status"
                        ).authenticated()
                        // === KẾT THÚC SỬA LỖI ===

                        // ===== 3. ADMIN ROUTES =====
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ===== 4. OWNER ROUTES =====
                        .requestMatchers("/api/v1/properties/submit-application").hasRole("OWNER")
                        .requestMatchers("/api/v1/owner/**").hasRole("OWNER")

                        // ===== 5. CUSTOMER ROUTES =====
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/applications/owner/**").hasRole("CUSTOMER")

                        // ===== 6. MIXED/COMBO ROUTES (Owner & Admin) =====
                        .requestMatchers("/api/v1/properties/add").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/properties/update/**").hasAnyRole("OWNER", "ADMIN")
                        .requestMatchers("/api/v1/room/**").hasAnyRole("OWNER", "ADMIN")

                        // ===== 7. DEFAULT =====
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}