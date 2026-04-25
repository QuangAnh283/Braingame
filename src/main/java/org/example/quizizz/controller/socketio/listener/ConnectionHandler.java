package org.example.quizizz.controller.socketio.listener;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.example.quizizz.controller.socketio.session.SessionManager;
import org.example.quizizz.model.entity.Room;
import org.example.quizizz.repository.RoomRepository;
import org.example.quizizz.security.JwtUtil;
import org.example.quizizz.service.Interface.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Xử lý kết nối và ngắt kết nối
 * - Xác thực JWT khi kết nối
 * - Quản lý phiên người dùng
 * - Thông báo ngắt kết nối tạm thời
 * - Giữ nguyên trạng thái phòng khi ngắt kết nối tạm thời
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectionHandler {

    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;
    private final RoomRepository roomRepository;
    private final IRedisService redisService;

    public void registerListeners(SocketIOServer server) {
        server.addConnectListener(onConnect());
        server.addDisconnectListener(onDisconnect(server));
    }

    // Xử lý sự kiện kết nối
    private ConnectListener onConnect() {
        return client -> {
            String token = resolveToken(client.getHandshakeData());
            log.info("🔌 Client {} attempting to connect with token: {}",
                    client.getSessionId(), token != null ? "present" : "missing");

            if (isTokenAllowed(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                sessionManager.addUserSession(client.getSessionId().toString(), userId);

                // Gửi xác nhận kết nối thành công
                client.sendEvent("connection-confirmed", Map.of(
                        "userId", userId,
                        "sessionId", client.getSessionId().toString(),
                        "timestamp", System.currentTimeMillis()));
            } else {
                client.disconnect();
            }
        };
    }

    private boolean isTokenAllowed(String token) {
        if (!jwtUtil.validateToken(token) || !jwtUtil.isAccessToken(token) || redisService.isTokenBlacklisted(token)) {
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        return jwtUtil.getTokenVersion(token) == redisService.getUserTokenVersion(userId);
    }

    private String resolveToken(com.corundumstudio.socketio.HandshakeData data) {
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

    // Xử lý sự kiện ngắt kết nối
    private DisconnectListener onDisconnect(SocketIOServer server) {
        return client -> {
            String sessionId = client.getSessionId().toString();
            Long userId = sessionManager.getUserId(sessionId);
            Long roomId = sessionManager.getRoomId(sessionId);

            if (userId != null && roomId != null) {
                Room room = roomRepository.findById(roomId).orElse(null);
                if (room != null) {
                    server.getRoomOperations("room-" + room.getRoomCode())
                            .sendEvent("player-disconnected", Map.of(
                                    "userId", userId,
                                    "temporary", true));
                }
                log.info("User {} disconnected from room {} (temporary)", userId, roomId);
            }

            // Xóa phiên người dùng
            sessionManager.removeSession(sessionId);
        };
    }
}
