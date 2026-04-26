package org.example.quizizz.repository;

import org.example.quizizz.model.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    List<UserAnswer> findByRoomId(Long roomId);
    List<UserAnswer> findByRoomIdAndUserId(Long roomId, Long userId);
    List<UserAnswer> findByUserId(Long userId);
    boolean existsByRoomIdAndUserIdAndQuestionId(Long roomId, Long userId, Long questionId);

    @Query("SELECT ua.userId, COUNT(ua) FROM UserAnswer ua WHERE ua.roomId = :roomId GROUP BY ua.userId")
    List<Object[]> countAnswersByUserInRoom(@Param("roomId") Long roomId);

    @Query("SELECT t.name, SUM(CASE WHEN ua.isCorrect = true THEN 1 ELSE 0 END), COUNT(ua) " +
            "FROM UserAnswer ua " +
            "JOIN Question q ON ua.questionId = q.id " +
            "JOIN Exam e ON q.examId = e.id " +
            "JOIN Topic t ON e.topicId = t.id " +
            "WHERE ua.userId = :userId " +
            "GROUP BY t.id, t.name " +
            "ORDER BY (SUM(CASE WHEN ua.isCorrect = true THEN 1 ELSE 0 END) * 1.0 / COUNT(ua)) DESC, COUNT(ua) DESC")
    List<Object[]> findTopicAccuracyByUserId(@Param("userId") Long userId);
}
