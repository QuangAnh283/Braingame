package org.example.quizizz.repository;

import org.example.quizizz.model.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    List<GameHistory> findByUserId(Long userId);

    // Find all game histories by game session id
    List<GameHistory> findByGameSessionId(Long gameSessionId);
    
    // Check if history exists for user in game session
    boolean existsByGameSessionIdAndUserId(Long gameSessionId, Long userId);

    // Get leaderboard data by topic
    @Query("SELECT gh.userId, AVG(gh.score) as avgScore, COUNT(gh) as gamesPlayed " +
           "FROM GameHistory gh " +
           "JOIN GameSession gs ON gh.gameSessionId = gs.id " +
           "JOIN Room r ON gs.roomId = r.id " +
           "WHERE r.topicId = :topicId " +
           "GROUP BY gh.userId " +
           "ORDER BY avgScore DESC")
    List<Object[]> findLeaderboardByTopicId(@Param("topicId") Long topicId);

    @Query(value = """
            SELECT ranked.game_session_id, ranked.user_rank
            FROM (
                SELECT gh.game_session_id,
                       gh.user_id,
                       RANK() OVER (PARTITION BY gh.game_session_id ORDER BY gh.score DESC) AS user_rank
                FROM game_histories gh
                WHERE gh.game_session_id IN (
                    SELECT user_gh.game_session_id
                    FROM game_histories user_gh
                    WHERE user_gh.user_id = :userId
                )
            ) ranked
            WHERE ranked.user_id = :userId
            """, nativeQuery = true)
    List<Object[]> findSessionRanksByUserId(@Param("userId") Long userId);
}
