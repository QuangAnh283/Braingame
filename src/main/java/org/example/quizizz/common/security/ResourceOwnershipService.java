package org.example.quizizz.common.security;

import lombok.RequiredArgsConstructor;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.model.entity.Answer;
import org.example.quizizz.model.entity.Exam;
import org.example.quizizz.model.entity.Question;
import org.example.quizizz.repository.AnswerRepository;
import org.example.quizizz.repository.ExamRepository;
import org.example.quizizz.repository.QuestionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ResourceOwnershipService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public void assertCanManageExam(Long examId, Long userId, String typeAccount) {
        if (isAdmin(typeAccount)) {
            return;
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.NOT_FOUND, "Exam not found"));

        if (!exam.getTeacherId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), MessageCode.FORBIDDEN, "You can only manage your own exam");
        }
    }

    public void assertCanManageQuestion(Long questionId, Long userId, String typeAccount) {
        if (isAdmin(typeAccount)) {
            return;
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.NOT_FOUND, "Question not found"));
        assertCanManageExam(question.getExamId(), userId, typeAccount);
    }

    public void assertCanManageQuestions(Collection<Long> questionIds, Long userId, String typeAccount) {
        if (questionIds == null) {
            return;
        }
        for (Long questionId : questionIds) {
            assertCanManageQuestion(questionId, userId, typeAccount);
        }
    }

    public void assertCanManageAnswer(Long answerId, Long userId, String typeAccount) {
        if (isAdmin(typeAccount)) {
            return;
        }

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), MessageCode.NOT_FOUND, "Answer not found"));
        assertCanManageQuestion(answer.getQuestionId(), userId, typeAccount);
    }

    public void requireScopedQuestionSearch(Long questionId, Long userId, String typeAccount) {
        if (isAdmin(typeAccount)) {
            return;
        }
        if (questionId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), MessageCode.FORBIDDEN, "Question scope is required");
        }
        assertCanManageQuestion(questionId, userId, typeAccount);
    }

    public boolean isAdmin(String typeAccount) {
        return "ADMIN".equalsIgnoreCase(typeAccount);
    }
}
