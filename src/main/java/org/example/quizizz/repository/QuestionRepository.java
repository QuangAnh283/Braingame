package org.example.quizizz.repository;

import org.example.quizizz.model.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamId(Long examId);
    List<Question> findByExamIdAndQuestionType(Long examId, String questionType);
    List<Question> findByQuestionType(String questionType);
    
    @Query(value = "SELECT * FROM questions WHERE exam_id = :examId AND question_type = :questionType ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestionsByExamAndType(@Param("examId") Long examId, @Param("questionType") String questionType, @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM questions WHERE exam_id = :examId ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestionsByExam(@Param("examId") Long examId, @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM questions WHERE question_type = :questionType ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestionsByType(@Param("questionType") String questionType, @Param("limit") int limit);

    @Query(value = "SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Question> findRandomQuestions(@Param("limit") int limit);
    
    long countByExamIdAndQuestionType(Long examId, String questionType);
    long countByExamId(Long examId);
    
    Page<Question> findByQuestionTextContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Question> findByExamIdAndQuestionTextContainingIgnoreCase(Long examId, String keyword, Pageable pageable);
    Page<Question> findByQuestionTypeAndQuestionTextContainingIgnoreCase(String questionType, String keyword, Pageable pageable);
    Page<Question> findByExamIdAndQuestionType(Long examId, String questionType, Pageable pageable);
    Page<Question> findByExamIdAndQuestionTypeAndQuestionTextContainingIgnoreCase(Long examId, String questionType, String keyword, Pageable pageable);
    Page<Question> findByExamIdIn(List<Long> examIds, Pageable pageable);
    Page<Question> findByExamIdInAndQuestionTextContainingIgnoreCase(List<Long> examIds, String keyword, Pageable pageable);
    Page<Question> findByExamIdInAndQuestionType(List<Long> examIds, String questionType, Pageable pageable);
    Page<Question> findByExamIdInAndQuestionTypeAndQuestionTextContainingIgnoreCase(List<Long> examIds, String questionType, String keyword, Pageable pageable);
    void deleteByExamId(Long examId);
}
