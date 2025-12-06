package com.example.smart_booking_system.security.oauth2;

import com.example.smart_booking_system.security.JwtTokenProvider;
import com.example.smart_booking_system.security.CustomUserDetails;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    @Override // Thêm Override cho rõ ràng
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Lấy UserDetails để kiểm tra trạng thái
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String token = tokenProvider.generateToken(authentication);

        // Xây dựng URL cơ bản
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", token);

        // === [LOGIC MỚI] Kiểm tra trạng thái User ===
        // Nếu user đang thiếu email (PENDING_EMAIL), báo hiệu cho Frontend biết
        if ("PENDING_EMAIL".equals(userDetails.getStatus())) {
            uriBuilder.queryParam("action", "require_email");
        }
        // ==========================================

        return uriBuilder.build().toUriString();
    }
}