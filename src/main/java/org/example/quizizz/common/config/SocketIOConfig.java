package org.example.quizizz.common.config;

import com.corundumstudio.socketio.AuthorizationResult;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quizizz.common.security.ClientIpResolver;
import org.example.quizizz.common.security.RateLimitService;
import org.example.quizizz.security.JwtUtil;
import org.example.quizizz.service.Interface.IRedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class SocketIOConfig {

    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;
    private final IRedisService redisService;
    private final ClientIpResolver clientIpResolver;

    @Value("${socketio.server.hostname:localhost}")
    private String host;

    @Value("${socketio.server.port:9093}")
    private Integer port;

    @Value("${socketio.server.cors.allowed-origins:${app.security.cors.allowed-origins:http://localhost:5173,http://localhost:8080}}")
    private String allowedOrigins;

    /**
     * Cấu hình Socket.IO server.
     * @return Socket.IO server
     */
    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(port);
        config.setOrigin(null);

        // Cấp phép quyền truy cập
        config.setTransports(com.corundumstudio.socketio.Transport.WEBSOCKET, com.corundumstudio.socketio.Transport.POLLING);

        // Cấu hình  timeout
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);
        config.setRandomSession(true);

        // Ping configuration
        config.setPingTimeout(60000);
        config.setPingInterval(25000);

        // Authorization listener to check token on connection
        config.setAuthorizationListener(data -> {
            String token = resolveToken(data);
            String origin = data.getHttpHeaders() != null ? data.getHttpHeaders().get("Origin") : null;
            boolean rateAllowed = isRateAllowed(data);
            boolean tokenValid = isTokenAllowed(token);
            boolean originAllowed = isOriginAllowed(origin);

            if (!rateAllowed || !tokenValid || !originAllowed) {
                log.warn("Rejected Socket.IO handshake. rateAllowed={}, tokenValid={}, origin={}", rateAllowed, tokenValid, origin);
                return new AuthorizationResult(false);
            }

            return new AuthorizationResult(true);
        });

        // Cấu hình Jackson để hỗ trợ Java 8 Date/Time API
        config.setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()));

        log.info("Socket.IO server configured on {}:{}", host, port);

        return new SocketIOServer(config);
    }

    private boolean isOriginAllowed(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }

        Set<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());

        return origins.contains(origin);
    }

    private boolean isTokenAllowed(String token) {
        if (!jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token) || redisService.isTokenBlacklisted(token)) {
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        return jwtUtil.getTokenVersion(token) == redisService.getUserTokenVersion(userId);
    }

    private String resolveToken(HandshakeData data) {
        String token = data.getSingleUrlParam("token");
        if (token != null && !token.isBlank()) {
            return token;
        }
        if (data.getHttpHeaders() == null) {
            return null;
        }
        String cookieHeader = data.getHttpHeaders().get("Cookie");
        if (cookieHeader == null || cookieHeader.isBlank()) {
            return null;
        }
        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && "accessToken".equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private boolean isRateAllowed(HandshakeData data) {
        String address = clientIpResolver.resolve(data);
        return rateLimitService.allow("socket:handshake:" + address, 60, Duration.ofMinutes(1));
    }
}
