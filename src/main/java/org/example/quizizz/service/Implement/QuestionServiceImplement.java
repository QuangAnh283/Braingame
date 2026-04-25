package org.example.quizizz.service.Implement;

import org.example.quizizz.mapper.AnswerMapper;
import org.example.quizizz.mapper.QuestionMapper;
import org.example.quizizz.model.dto.question.*;
import org.example.quizizz.model.entity.Answer;
import org.example.quizizz.model.entity.Question;
import org.example.quizizz.repository.AnswerRepository;
import org.example.quizizz.repository.QuestionRepository;
import org.example.quizizz.service.Interface.IQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImplement implements IQuestionService {

    private static final int MAX_RANDOM_QUESTIONS = 50;
    
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final QuestionMapper questionMapper;
    private final AnswerMapper answerMapper;
    private final org.example.quizizz.repository.ExamRepository examRepository;
    
    @Override
    public org.example.quizizz.repository.ExamRepository getExamRepository() {
        return examRepository;
    }

    @Override
    public List<QuestionWithAnswersResponse> getRandomQuestionsWithAnswers(Long examId, String questionType, int count) {
        List<Question> questions = getRandomQuestions(examId, questionType, sanitizeCount(count));
        return questions.stream().map(this::mapToQuestionWithAnswers).toList();
    }

    @Override
    public List<QuestionWithAnswersResponse> getRandomQuestionsForPlayer(Long examId, String questionType, int count, Long playerId) {
        List<Question> allQuestions = getAllQuestions(examId, questionType);
        int sanitizedCount = sanitizeCount(count);
        
        // Shuffle based on playerId for consistent randomization per player
        Random random = new Random(playerId.hashCode());
        Collections.shuffle(allQuestions, random);
        
        List<Question> playerQuestions = allQuestions.stream().limit(sanitizedCount).toList();
        return playerQuestions.stream().map(this::mapToQuestionWithAnswers).toList();
    }

    @Override
    public long countAvailableQuestions(Long examId, String questionType) {
        if (examId != null && questionType != null) {
            return questionRepository.countByExamIdAndQuestionType(examId, questionType);
        } else if (examId != null) {
            return questionRepository.countByExamId(examId);
        }
        return questionRepository.count();
    }

    private List<Question> getRandomQuestions(Long examId, String questionType, int count) {
        if (examId != null && questionType != null) {
            return questionRepository.findRandomQuestionsByExamAndType(examId, questionType, count);
        } else if (examId != null) {
            return questionRepository.findRandomQuestionsByExam(examId, count);
        } else if (questionType != null) {
            return questionRepository.findRandomQuestionsByType(questionType, count);
        }
        return questionRepository.findRandomQuestions(count);
    }

    private List<Question> getAllQuestions(Long examId, String questionType) {
        if (examId != null && questionType != null) {
            return questionRepository.findByExamIdAndQuestionType(examId, questionType);
        } else if (examId != null) {
            return questionRepository.findByExamId(examId);
        } else if (questionType != null) {
            return questionRepository.findByQuestionType(questionType);
        }
        return questionRepository.findAll();
    }

    private QuestionWithAnswersResponse mapToQuestionWithAnswers(Question question) {
        List<Answer> answers = answerRepository.findByQuestionId(question.getId());
        return new QuestionWithAnswersResponse(
            question.getId(),
            question.getQuestionText(),
            question.getExamId(),
            question.getQuestionType(),
            answers.stream().map(answerMapper::toResponse).collect(Collectors.toList())
        );
    }

    @Override
    @CacheEvict(value = {"questions", "questionsByTopic"}, allEntries = true)
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        Question question = questionMapper.toEntity(request);
        Question savedQuestion = questionRepository.save(question);
        return questionMapper.toResponse(savedQuestion);
    }

    @Override
    @CacheEvict(value = {"questions", "questionsByTopic"}, allEntries = true)
    public List<QuestionResponse> createBulkQuestions(CreateBulkQuestionsRequest request) {
        List<Question> questions = request.getQuestions().stream()
                .map(questionMapper::toEntity)
                .collect(Collectors.toList());
        List<Question> savedQuestions = questionRepository.saveAll(questions);
        return savedQuestions.stream()
                .map(questionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CachePut(value = "question", key = "#id")
    @CacheEvict(value = {"questions", "questionsByTopic"}, allEntries = true)
    public QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        questionMapper.updateEntityFromRequest(question, request);
        Question updatedQuestion = questionRepository.save(question);
        return questionMapper.toResponse(updatedQuestion);
    }

    @Override
    @CacheEvict(value = {"question", "questions", "questionsByTopic"}, allEntries = true)
    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    @Override
    @CacheEvict(value = {"question", "questions", "questionsByTopic"}, allEntries = true)
    public void deleteBulkQuestions(DeleteBulkQuestionsRequest request) {
        questionRepository.deleteAllById(request.getQuestionIds());
    }

    @Override
    @Cacheable(value = "question", key = "#id")
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return questionMapper.toResponse(question);
    }

    @Override
    @Cacheable(value = "questionsByExam", key = "#examId")
    public List<QuestionWithAnswersResponse> getQuestionsByExamId(Long examId) {
        List<Question> questions = questionRepository.findByExamId(examId);
        return questions.stream()
                .map(this::mapToQuestionWithAnswers)
                .collect(Collectors.toList());
    }

    @Override
    public Page<QuestionWithAnswersResponse> search(
            String keyword, Long examId, String questionType, Pageable pageable) {
        
        org.springframework.data.domain.Page<Question> questions;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        
        if (examId != null && questionType != null && hasKeyword) {
            questions = questionRepository.findByExamIdAndQuestionTypeAndQuestionTextContainingIgnoreCase(
                    examId, questionType, keyword, pageable);
        } else if (examId != null && questionType != null) {
            questions = questionRepository.findByExamIdAndQuestionType(examId, questionType, pageable);
        } else if (examId != null && hasKeyword) {
            questions = questionRepository.findByExamIdAndQuestionTextContainingIgnoreCase(
                    examId, keyword, pageable);
        } else if (questionType != null && hasKeyword) {
            questions = questionRepository.findByQuestionTypeAndQuestionTextContainingIgnoreCase(
                    questionType, keyword, pageable);
        } else if (hasKeyword) {
            questions = questionRepository.findByQuestionTextContainingIgnoreCase(keyword, pageable);
        } else {
            questions = questionRepository.findAll(pageable);
        }
        
        return questions.map(this::mapToQuestionWithAnswers);
    }

    @Override
    public org.example.quizizz.model.dto.PageResponse<QuestionWithAnswersResponse> searchByTeacher(
            String keyword, Long examId, String questionType, Long teacherId, Pageable pageable) {
        
        java.util.List<Long> teacherExamIds = examRepository.findByTeacherId(teacherId)
            .stream()
            .map(org.example.quizizz.model.entity.Exam::getId)
            .collect(Collectors.toList());
        
        if (teacherExamIds.isEmpty()) {
            return new org.example.quizizz.model.dto.PageResponse<>(
                java.util.Collections.emptyList(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0, true, true
            );
        }

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        Page<Question> questions;

        if (examId != null) {
            if (!teacherExamIds.contains(examId)) {
                questions = new PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
            } else if (questionType != null && hasKeyword) {
                questions = questionRepository.findByExamIdAndQuestionTypeAndQuestionTextContainingIgnoreCase(
                        examId, questionType, keyword, pageable);
            } else if (questionType != null) {
                questions = questionRepository.findByExamIdAndQuestionType(examId, questionType, pageable);
            } else if (hasKeyword) {
                questions = questionRepository.findByExamIdAndQuestionTextContainingIgnoreCase(examId, keyword, pageable);
            } else {
                questions = questionRepository.findByExamIdIn(java.util.List.of(examId), pageable);
            }
        } else if (questionType != null && hasKeyword) {
            questions = questionRepository.findByExamIdInAndQuestionTypeAndQuestionTextContainingIgnoreCase(
                    teacherExamIds, questionType, keyword, pageable);
        } else if (questionType != null) {
            questions = questionRepository.findByExamIdInAndQuestionType(teacherExamIds, questionType, pageable);
        } else if (hasKeyword) {
            questions = questionRepository.findByExamIdInAndQuestionTextContainingIgnoreCase(teacherExamIds, keyword, pageable);
        } else {
            questions = questionRepository.findByExamIdIn(teacherExamIds, pageable);
        }

        return org.example.quizizz.model.dto.PageResponse.of(questions.map(this::mapToQuestionWithAnswers));
    }

    private int sanitizeCount(int count) {
        return Math.max(1, Math.min(count, MAX_RANDOM_QUESTIONS));
    }
}
