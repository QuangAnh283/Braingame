package org.example.quizizz.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.common.security.ClientIpResolver;
import org.example.quizizz.common.security.RateLimitService;
import org.example.quizizz.common.security.ResourceOwnershipService;
import org.example.quizizz.model.dto.ai.AIGenerateRequest;
import org.example.quizizz.model.dto.ai.AIGenerateResponse;
import org.example.quizizz.security.JwtAuthenticationToken;
import org.example.quizizz.service.Interface.IAIService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "8. Trợ lý AI", description = "API tạo câu hỏi tự động bằng AI")
public class AIController {
    
    private final IAIService aiServiceImpl;
    private final RateLimitService rateLimitService;
    private final ResourceOwnershipService resourceOwnershipService;
    private final ClientIpResolver clientIpResolver;
    
    /**
     * Tạo câu hỏi và đáp án bằng AI
     */
    @PostMapping("/generate-questions")
    @PreAuthorize("hasAuthority('question:manage')")
    @Operation(
        summary = "Tạo câu hỏi + đáp án bằng AI",
        description = "Giáo viên nhập mô tả tự nhiên, AI tự động tạo câu hỏi và đáp án"
    )
    public ResponseEntity<ApiResponse<AIGenerateResponse>> generateQuestions(
            @RequestBody @Valid AIGenerateRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        Long userId = (Long) authentication.getPrincipal();
        String typeAccount = ((JwtAuthenticationToken) authentication).getTypeAccount();
        enforceRateLimit("ai:generate:" + userId + ":" + clientKey(servletRequest), 10, Duration.ofMinutes(1));
        resourceOwnershipService.assertCanManageExam(request.getExamId(), userId, typeAccount);
        
        AIGenerateResponse response = aiServiceImpl.generateQuestionsFromNaturalLanguage(
            request.getExamId(),
            request.getUserPrompt()
        );
        
        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, response));
    }

    private void enforceRateLimit(String key, int maxRequests, Duration window) {
        if (!rateLimitService.allow(key, maxRequests, window)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS.value(), MessageCode.OPERATION_NOT_ALLOWED, "Too many requests");
        }
    }

    private String clientKey(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
