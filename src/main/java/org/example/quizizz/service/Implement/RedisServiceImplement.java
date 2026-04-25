package org.example.quizizz.service.Implement;

import lombok.RequiredArgsConstructor;
import org.example.quizizz.common.cache.CacheLookupResult;
import org.example.quizizz.common.cache.domain.auth.TokenBlacklistCache;
import org.example.quizizz.common.cache.domain.game.GameSessionCache;
import org.example.quizizz.common.cache.domain.presence.OnlineUserCache;
import org.example.quizizz.common.cache.domain.user.UserPermissionCache;
import org.example.quizizz.common.constants.GameStatus;
import org.example.quizizz.common.constants.PermissionCode;
import org.example.quizizz.service.Interface.IRedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisServiceImplement implements IRedisService {

    private static final String TOKEN_VERSION_KEY_PREFIX = "auth:token-version:";

    private final UserPermissionCache userPermissionCache;
    private final GameSessionCache gameSessionCache;
    private final TokenBlacklistCache tokenBlacklistCache;
    private final OnlineUserCache onlineUserCache;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Lưu quyền cho người dùng
     * @param userId
     * @param permissions
     */
    @Override
    public void saveUserPermissions(Long userId, Set<PermissionCode> permissions) {
        userPermissionCache.save(userId, permissions);
    }

    /**
     * Lấy quyền của người dùng
     * @param userId
     * @return
     */
    @Override
    public Set<PermissionCode> getUserPermissions(Long userId) {
        return userPermissionCache.get(userId);
    }

    @Override
    public void deleteUserPermissionsCache(Long userId) {
        userPermissionCache.evict(userId);
    }

    /**
     * Lưu game session
     * @param gameId
     * @param sessionData
     */
    @Override
    public void saveGameSession(String gameId, Map<String, Object> sessionData) {
        gameSessionCache.save(gameId, sessionData);
    }

    /**
     * Lấy game session
     * @param gameId
     * @return
     */
    @Override
    public Map<String, Object> getGameSession(String gameId) {
        CacheLookupResult<Map<String, Object>> result = gameSessionCache.get(gameId);
        if (result.isHit()) {
            return result.getValue();
        }
        return Collections.emptyMap();
    }

    /**
     * Cập nhật game session
     * @param gameId
     * @param field
     * @param value
     */
    @Override
    public void updateGameSession(String gameId, String field, Object value) {
        gameSessionCache.updateField(gameId, field, value);
    }

    /**
     * Cập nhật trạng thái game
     * @param gameId
     * @param status
     */
    @Override
    public void updateGameStatus(String gameId, GameStatus status) {
        gameSessionCache.updateStatus(gameId, status);
    }

    @Override
    public void addTokenToBlacklistWithRefreshTTL(String token, long refreshTokenExpiration) {
        tokenBlacklistCache.blacklist(token, refreshTokenExpiration);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistCache.isBlacklisted(token);
    }

    @Override
    public long getUserTokenVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Object value = redisTemplate.opsForValue().get(TOKEN_VERSION_KEY_PREFIX + userId);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Long.parseLong(string);
        }
        return 0L;
    }

    @Override
    public long incrementUserTokenVersion(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Long value = redisTemplate.opsForValue().increment(TOKEN_VERSION_KEY_PREFIX + userId);
        return value != null ? value : 0L;
    }

    // ============ USER ONLINE STATUS ============
    @Override
    public void setUserOnline(Long userId) {
        onlineUserCache.markOnline(userId);
    }

    @Override
    public void setUserOffline(Long userId) {
        onlineUserCache.markOffline(userId);
    }
}
