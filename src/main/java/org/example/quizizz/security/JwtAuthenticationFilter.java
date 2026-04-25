package org.example.quizizz.security;

import org.example.quizizz.common.constants.PermissionCode;
import org.example.quizizz.repository.PermissionRepository;
import org.example.quizizz.service.Interface.IRedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final IRedisService redisService;
    private final PermissionRepository permissionRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null && !token.isBlank()) {
            try {
                if (!jwtUtil.validateToken(token)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token không hợp lệ");
                    return;
                }
                if (!jwtUtil.isAccessToken(token)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token không đúng loại");
                    return;
                }
                if (redisService.isTokenBlacklisted(token)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token đã bị thu hồi");
                    return;
                }
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (jwtUtil.getTokenVersion(token) != redisService.getUserTokenVersion(userId)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token đã bị thu hồi");
                    return;
                }
                String typeAccount = jwtUtil.getClaimFromToken(token, claims -> claims.get("typeAccount", String.class));
                Collection<SimpleGrantedAuthority> authorities = getAuthoritiesFromRedis(userId);

                JwtAuthenticationToken authentication = new JwtAuthenticationToken(userId, typeAccount, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token không hợp lệ");
                return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Thiếu token xác thực");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }

        token = request.getHeader("accessToken");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        if (token != null && !token.isBlank()) {
            return token;
        }

        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        if (path.startsWith("/api/v1/auth/logout") || path.startsWith("/api/v1/auth/change-password")) {
            return false;
        }
        if (path.startsWith("/api/v1/auth")) {
            return true;
        }
        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
            return true;
        }
        if (path.startsWith("/actuator/")) {
            return false;
        }
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.equals("/swagger-ui.html") ||
                path.equals("/error") ||
                !path.startsWith("/api/");
    }

    private Collection<SimpleGrantedAuthority> getAuthoritiesFromRedis(Long userId) {
        Set<PermissionCode> permissions = redisService.getUserPermissions(userId);

        // Fallback to DB if cache is missed/unavailable so auth does not break when Redis has issues.
        if (permissions == null || permissions.isEmpty()) {
            permissions = loadPermissionsFromDatabase(userId);
            if (!permissions.isEmpty()) {
                redisService.saveUserPermissions(userId, permissions);
            }
        }

        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        return permissions.stream()
                .map(PermissionCode::getCode)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private Set<PermissionCode> loadPermissionsFromDatabase(Long userId) {
        try {
            return permissionRepository.findPermissionsByUserId(userId)
                    .stream()
                    .map(permission -> PermissionCode.fromCode(permission.getPermissionName()).orElse(null))
                    .filter(permissionCode -> permissionCode != null)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to load permissions from database for userId={}", userId, e);
            return Collections.emptySet();
        }
    }
}
