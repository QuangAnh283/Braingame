package org.example.quizizz.controller.api;

import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.common.security.RateLimitService;
import org.example.quizizz.model.dto.authentication.*;
import org.example.quizizz.service.Interface.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "APIs liên quan đến đăng nhập, đăng ký")
public class AuthController {

    private final IAuthService authService;
    private final RateLimitService rateLimitService;
    private final org.example.quizizz.common.security.ClientIpResolver clientIpResolver;

    @Value("${app.security.cookies.secure:false}")
    private boolean secureCookies;

    @Value("${app.security.cookies.domain:}")
    private String cookieDomain;

    @Operation(summary = "Đăng ký tài khoản", description = "Đăng ký tài khoản mới cho người dùng")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.USER_CREATED, response));
    }

    @Operation(summary = "Đăng nhập", description = "Đăng nhập hệ thống")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest servletRequest,
                                                            HttpServletResponse servletResponse) {
        enforceRateLimit("auth:login:" + clientKey(servletRequest) + ":" + request.getUsername(), 10, Duration.ofMinutes(1));
        LoginResponse response = authService.login(request);
        writeAuthCookies(servletResponse, response);
        response.setRefreshToken(null);
        response.setAccessToken(null);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_LOGIN_SUCCESS, response));
    }

    @Operation(summary = "Đăng xuất", description = "Đăng xuất khỏi hệ thống")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        String token = resolveAccessToken(authHeader, request);
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), MessageCode.AUTH_INVALID_TOKEN, "Invalid authorization header");
        }
        authService.logout(token);
        clearAuthCookies(response);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_LOGOUT_SUCCESS, "Logout successful"));
    }

    @Operation(summary = "Làm mới token", description = "Lấy access token mới từ refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody(required = false) String refreshToken,
                                                                   HttpServletRequest servletRequest,
                                                                   HttpServletResponse servletResponse) {
        enforceRateLimit("auth:refresh:" + clientKey(servletRequest), 30, Duration.ofMinutes(1));
        String token = refreshToken != null && !refreshToken.isBlank()
                ? refreshToken
                : resolveCookie(servletRequest, "refreshToken");
        LoginResponse response = authService.refreshToken(token);
        writeAuthCookies(servletResponse, response);
        response.setRefreshToken(null);
        response.setAccessToken(null);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_TOKEN_REFRESHED, response));
    }

    @Operation(summary = "Reset mật khẩu", description = "Reset mật khẩu người dùng qua email và tự động đăng xuất tất cả thiết bị")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                                            HttpServletRequest servletRequest) {
        enforceRateLimit("auth:reset:" + clientKey(servletRequest) + ":" + request.getEmail(), 5, Duration.ofHours(1));
        ResetPasswordResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_PASSWORD_RESET_SUCCESS, response));
    }

    @Operation(summary = "Xác nhận reset mật khẩu", description = "Đặt mật khẩu mới bằng token một lần")
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> confirmResetPassword(
            @Valid @RequestBody ConfirmResetPasswordRequest request,
            HttpServletRequest servletRequest) {
        enforceRateLimit("auth:reset-confirm:" + clientKey(servletRequest), 10, Duration.ofMinutes(10));
        ResetPasswordResponse response = authService.confirmResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_PASSWORD_RESET_SUCCESS, response));
    }

    @Operation(summary = "Đổi mật khẩu", description = "Đổi mật khẩu cho người dùng hiện tại")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication auth) {
        Long userId = Long.valueOf(auth.getName());
        ChangePasswordResponse response = authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_PASSWORD_CHANGED, response));
    }

    @Operation(summary = "Xác thực email", description = "Xác thực email sau khi đăng ký")
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        boolean verified = authService.verifyEmail(token);
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_EMAIL_VERIFIED, "Email verified successfully"));
        }
        return ResponseEntity.badRequest().build();
    }

    @Operation(summary = "Quên mật khẩu", description = "Gửi link đặt lại mật khẩu qua email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> forgotPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                                             HttpServletRequest servletRequest) {
        enforceRateLimit("auth:forgot:" + clientKey(servletRequest) + ":" + request.getEmail(), 5, Duration.ofHours(1));
        ResetPasswordResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.AUTH_PASSWORD_RESET_SUCCESS, response));
    }

    private void enforceRateLimit(String key, int maxRequests, Duration window) {
        if (!rateLimitService.allow(key, maxRequests, window)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS.value(), MessageCode.OPERATION_NOT_ALLOWED, "Too many requests");
        }
    }

    private String clientKey(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }

    private void writeAuthCookies(HttpServletResponse response, LoginResponse loginResponse) {
        addCookie(response, "accessToken", loginResponse.getAccessToken(), Duration.ofMillis(24 * 60 * 60 * 1000L));
        addCookie(response, "refreshToken", loginResponse.getRefreshToken(), Duration.ofDays(7));
    }

    private void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, "accessToken", "", Duration.ZERO);
        addCookie(response, "refreshToken", "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        response.addHeader("Set-Cookie", builder.build().toString());
    }

    private String resolveAccessToken(String authHeader, HttpServletRequest request) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return resolveCookie(request, "accessToken");
    }

    private String resolveCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
