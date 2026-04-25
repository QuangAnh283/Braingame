package org.example.quizizz.service.Interface;

import org.example.quizizz.common.constants.GameStatus;
import org.example.quizizz.common.constants.PermissionCode;

import java.util.Map;
import java.util.Set;

public interface IRedisService {

    /**
     * User permissions
     */
    void saveUserPermissions(Long userId, Set<PermissionCode> permissions);

    Set<PermissionCode> getUserPermissions(Long userId);

    void deleteUserPermissionsCache(Long userId);

    /**
     * Game session
     */
    void saveGameSession(String gameId, Map<String, Object> sessionData);

    Map<String, Object> getGameSession(String gameId);

    void updateGameSession(String gameId, String field, Object value);

    void updateGameStatus(String gameId, GameStatus status);


    /**
     * Token blacklist
     */
    void addTokenToBlacklistWithRefreshTTL(String token, long refreshTokenExpiration);

    boolean isTokenBlacklisted(String token);

    long getUserTokenVersion(Long userId);

    long incrementUserTokenVersion(Long userId);

    /**
     * Online users
     */
    void setUserOnline(Long userId);

    void setUserOffline(Long userId);
}
