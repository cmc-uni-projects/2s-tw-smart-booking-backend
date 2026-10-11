package com.example.smart_booking_system.security;

import com.example.smart_booking_system.i18n.Messages;
import com.example.smart_booking_system.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final Messages messages;

    private final JwtTokenProvider jwtUtil; // JWT helper
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // kiểm tra token đã bị thu hồi chưa
                if (tokenBlacklistService.isBlacklisted(token)) {
                    logger.info("Rejected blacklisted token");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Token is blacklisted. Please login again.\"}");
                    return;
                }

                // nếu chưa bị thu hồi thì kiểm tra token có hợp lệ không
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserIdFromToken(token);

                    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserById(userId);
                        if (!userDetails.isAccountNonLocked()) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(jsonError("auth.lockedOrSuspended", messages.t("auth.lockedOrSuspended")));
                            return; // Dừng request ngay lập tức
                        }

                        if (!userDetails.isEnabled()) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(jsonError("auth.notActivated", messages.t("auth.notActivated")));
                            return;
                        }
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }


            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            logger.error("JWT authentication failed", ex);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid token.\"}");
        }

    }

    /** Same shape as ApiResponse errors: translated message plus its stable i18n key. */
    private static String jsonError(String code, String message) {
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"success\": false, \"message\": \"" + escaped + "\", \"error\": \"" + escaped + "\", \"code\": \"" + code + "\"}";
    }
}
