package org.example.quizizz.controller.api;

import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.security.ResourceOwnershipService;
import org.example.quizizz.model.dto.PageResponse;
import org.example.quizizz.model.dto.question.*;
import org.example.quizizz.security.JwtAuthenticationToken;
import org.example.quizizz.service.Interface.IQuestionService;
import org.example.quizizz.util.PageableUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "6. Câu hỏi", description = "API liên quan đến câu hỏi")
public class QuestionController {

    private final IQuestionService questionService;
    private final ResourceOwnershipService resourceOwnershipService;

    @Operation(summary = "Lấy câu hỏi ngẫu nhiên với đáp án", description = "Lấy câu hỏi ngẫu nhiên kèm đáp án theo chủ đề và loại câu hỏi")
    @GetMapping("/random")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<List<QuestionWithAnswersResponse>>> getRandomQuestions(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "10") int count,
            Authentication authentication) {

        assertScopedExamIfNeeded(examId, authentication);

        List<QuestionWithAnswersResponse> questions = questionService.getRandomQuestionsWithAnswers(examId, questionType, count);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, questions));
    }

    @Operation(summary = "Lấy câu hỏi cho người chơi cụ thể", description = "Lấy câu hỏi ngẫu nhiên khác nhau cho mỗi người chơi")
    @GetMapping("/random/player/{playerId}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<List<QuestionWithAnswersResponse>>> getRandomQuestionsForPlayer(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "10") int count,
            Authentication authentication) {

        assertScopedExamIfNeeded(examId, authentication);

        List<QuestionWithAnswersResponse> questions = questionService.getRandomQuestionsForPlayer(examId, questionType, count, playerId);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, questions));
    }

    @Operation(summary = "Đếm số câu hỏi có sẵn", description = "Đếm số câu hỏi có sẵn theo chủ đề và loại câu hỏi")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countAvailableQuestions(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String questionType) {

        long count = questionService.countAvailableQuestions(examId, questionType);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, count));
    }

    @Operation(summary = "Tạo câu hỏi mới", description = "Tạo một câu hỏi mới")
    @PostMapping
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(@Valid @RequestBody CreateQuestionRequest request,
                                                                        Authentication authentication) {
        assertCanManageExam(request.getExamId(), authentication);
        QuestionResponse response = questionService.createQuestion(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Tạo nhiều câu hỏi", description = "Tạo nhiều câu hỏi cùng lúc")
    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> createBulkQuestions(@Valid @RequestBody CreateBulkQuestionsRequest request,
                                                                                  Authentication authentication) {
        request.getQuestions().forEach(question -> assertCanManageExam(question.getExamId(), authentication));
        List<QuestionResponse> responses = questionService.createBulkQuestions(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, responses));
    }

    @Operation(summary = "Cập nhật câu hỏi", description = "Cập nhật thông tin câu hỏi theo ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable Long id,
            @RequestBody UpdateQuestionRequest request,
            Authentication authentication) {
        assertCanManageQuestion(id, authentication);
        QuestionResponse response = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Xóa câu hỏi", description = "Xóa câu hỏi theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id, Authentication authentication) {
        assertCanManageQuestion(id, authentication);
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, null));
    }

    @Operation(summary = "Xóa nhiều câu hỏi", description = "Xóa nhiều câu hỏi cùng lúc")
    @DeleteMapping("/bulk")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteBulkQuestions(@Valid @RequestBody DeleteBulkQuestionsRequest request,
                                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        resourceOwnershipService.assertCanManageQuestions(request.getQuestionIds(), userId, typeAccount);
        questionService.deleteBulkQuestions(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, null));
    }

    @Operation(summary = "Lấy câu hỏi theo ID", description = "Lấy thông tin chi tiết câu hỏi theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionById(@PathVariable Long id,
                                                                         Authentication authentication) {
        assertCanManageQuestion(id, authentication);
        QuestionResponse response = questionService.getQuestionById(id);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Lấy danh sách câu hỏi", description = "Lấy danh sách câu hỏi với phân trang, tìm kiếm và lọc theo chủ đề, loại câu hỏi (giáo viên chỉ xem của mình)")
    @GetMapping
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<PageResponse<QuestionWithAnswersResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        JwtAuthenticationToken auth = (JwtAuthenticationToken) authentication;
        String typeAccount = auth.getTypeAccount();
        
        PageResponse<QuestionWithAnswersResponse> response;
        
        if (!resourceOwnershipService.isAdmin(typeAccount)) {
            response = questionService.searchByTeacher(keyword, examId, questionType, userId, PageableUtil.createPageable(page, size, sort));
        } else {
            response = PageResponse.of(
                questionService.search(keyword, examId, questionType, PageableUtil.createPageable(page, size, sort)));
        }
        
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    private void assertScopedExamIfNeeded(Long examId, Authentication authentication) {
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        if (resourceOwnershipService.isAdmin(typeAccount)) {
            return;
        }
        if (examId == null) {
            throw new org.example.quizizz.common.exception.ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN.value(),
                    MessageCode.FORBIDDEN,
                    "Exam scope is required");
        }
        assertCanManageExam(examId, authentication);
    }

    private void assertCanManageExam(Long examId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        resourceOwnershipService.assertCanManageExam(examId, userId, typeAccount);
    }

    private void assertCanManageQuestion(Long questionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        resourceOwnershipService.assertCanManageQuestion(questionId, userId, typeAccount);
    }
}
