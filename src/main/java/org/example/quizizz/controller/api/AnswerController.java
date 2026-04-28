package org.example.quizizz.controller.api;

import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.security.ResourceOwnershipService;
import org.example.quizizz.model.dto.PageResponse;
import org.example.quizizz.model.dto.answer.*;
import org.example.quizizz.security.JwtAuthenticationToken;
import org.example.quizizz.service.Interface.IAnswerService;
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
@RequestMapping("/api/v1/answers")
@RequiredArgsConstructor
@Tag(name = "7. Đáp án", description = "API liên quan đến đáp án")
public class AnswerController {

    private final IAnswerService answerService;
    private final ResourceOwnershipService resourceOwnershipService;

    @Operation(summary = "Tạo đáp án mới", description = "Tạo một đáp án mới cho câu hỏi")
    @PostMapping
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<AnswerResponse>> createAnswer(@Valid @RequestBody CreateAnswerRequest request,
                                                                    Authentication authentication) {
        assertCanManageQuestion(request.getQuestionId(), authentication);
        AnswerResponse response = answerService.createAnswer(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Tạo nhiều đáp án", description = "Tạo nhiều đáp án cùng lúc")
    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> createBulkAnswers(@Valid @RequestBody CreateBulkAnswersRequest request,
                                                                              Authentication authentication) {
        request.getAnswers().forEach(answer -> assertCanManageQuestion(answer.getQuestionId(), authentication));
        List<AnswerResponse> responses = answerService.createBulkAnswers(request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, responses));
    }

    @Operation(summary = "Cập nhật đáp án", description = "Cập nhật thông tin đáp án theo ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<AnswerResponse>> updateAnswer(
            @PathVariable Long id,
            @RequestBody UpdateAnswerRequest request,
            Authentication authentication) {
        assertCanManageAnswer(id, authentication);
        AnswerResponse response = answerService.updateAnswer(id, request);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Xóa đáp án", description = "Xóa đáp án theo ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(@PathVariable Long id, Authentication authentication) {
        assertCanManageAnswer(id, authentication);
        answerService.deleteAnswer(id);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, null));
    }

    @Operation(summary = "Lấy đáp án theo ID", description = "Lấy thông tin chi tiết đáp án theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<AnswerResponse>> getAnswerById(@PathVariable Long id,
                                                                     Authentication authentication) {
        assertCanManageAnswer(id, authentication);
        AnswerResponse response = answerService.getAnswerById(id);
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    @Operation(summary = "Lấy danh sách đáp án", description = "Lấy danh sách đáp án với phân trang, tìm kiếm và lọc theo câu hỏi, trạng thái đúng/sai")
    @GetMapping
    @PreAuthorize("hasAuthority('question:manage')")
    public ResponseEntity<ApiResponse<PageResponse<AnswerResponse>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) Boolean isCorrect,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        resourceOwnershipService.requireScopedQuestionSearch(questionId, userId, typeAccount);
        
        PageResponse<AnswerResponse> response = PageResponse.of(
                answerService.search(keyword, questionId, isCorrect, PageableUtil.createPageable(page, size, sort)));
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    private void assertCanManageQuestion(Long questionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        resourceOwnershipService.assertCanManageQuestion(questionId, userId, typeAccount);
    }

    private void assertCanManageAnswer(Long answerId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        resourceOwnershipService.assertCanManageAnswer(answerId, userId, typeAccount);
    }
}
