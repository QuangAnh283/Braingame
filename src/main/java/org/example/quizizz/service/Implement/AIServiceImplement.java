package org.example.quizizz.service.Implement;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quizizz.model.dto.ai.*;
import org.example.quizizz.model.dto.answer.CreateAnswerRequest;
import org.example.quizizz.model.dto.answer.CreateBulkAnswersRequest;
import org.example.quizizz.model.dto.question.CreateBulkQuestionsRequest;
import org.example.quizizz.model.dto.question.CreateQuestionRequest;
import org.example.quizizz.model.dto.question.QuestionResponse;
import org.example.quizizz.model.dto.question.QuestionWithAnswersResponse;
import org.example.quizizz.model.dto.topic.TopicResponse;
import org.example.quizizz.service.Interface.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AIServiceImplement implements IAIService {
    
    private final RestClient openaiRestClient;
    private final ITopicService topicService;
    private final IExamService examService;
    private final IQuestionService questionService;
    private final IAnswerService answerService;
    private final ObjectMapper objectMapper;
    
    @Value("${gemini.model}")
    private String model;
    
    @Value("${gemini.api-key}")
    private String apiKey;
    
    @Override
    public AIGenerateResponse generateQuestionsFromNaturalLanguage(Long examId, String userPrompt) {
        log.info("Generating questions for exam {} with prompt: {}", examId, userPrompt);
        
        try {
            var exam = examService.getById(examId);
            TopicResponse topic = topicService.getById(exam.getTopicId());
            String systemPrompt = buildSystemPrompt(topic, userPrompt);
            
            String requestBody = buildGeminiRequest(systemPrompt, userPrompt);
            
            String response = openaiRestClient.post()
                    .uri("/models/" + model + ":generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            
            String jsonResponse = parseGeminiResponse(response);
            log.debug("AI Response: {}", jsonResponse);
            
            List<QuestionWithAnswersResponse> questions = parseAndSaveQuestions(jsonResponse, examId);
            
            return AIGenerateResponse.builder()
                .totalGenerated(questions.size())
                .questions(questions)
                .message("Đã tạo thành công " + questions.size() + " câu hỏi")
                .build();
        } catch (Exception e) {
            log.error("Error generating questions: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo câu hỏi: " + e.getMessage(), e);
        }
    }
    
    private String buildGeminiRequest(String systemPrompt, String userPrompt) {
        try {
            String combinedPrompt = systemPrompt + "\n\n" + userPrompt;
            return """
                {
                  "contents": [{
                    "parts": [{
                      "text": %s
                    }]
                  }],
                  "generationConfig": {
                    "temperature": 0.7,
                    "maxOutputTokens": 8192
                  }
                }
                """.formatted(objectMapper.writeValueAsString(combinedPrompt));
        } catch (Exception e) {
            throw new RuntimeException("Error building Gemini request", e);
        }
    }
    
    private String parseGeminiResponse(String response) {
        try {
            var jsonNode = objectMapper.readTree(response);
            return jsonNode.at("/candidates/0/content/parts/0/text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Gemini response", e);
        }
    }
    
    /**
     * Xây dựng system prompt cho AI
     */
    private String buildSystemPrompt(TopicResponse topic, String userPrompt) {
        return """
            Bạn là trợ lý tạo câu hỏi trắc nghiệm cho giáo viên.
            
            THÔNG TIN CHỦ ĐỀ:
            - Tên: %s
            - Mô tả: %s
            
            YÊU CẦU NGƯỜI DÙNG: %s
            
            NHIỆM VỤ:
            1. Phân tích yêu cầu của giáo viên (số lượng, độ khó, nội dung cụ thể)
            2. Tạo câu hỏi trắc nghiệm chất lượng cao
            3. Trả về ĐÚNG format JSON sau (KHÔNG thêm markdown, KHÔNG thêm text khác):
            
            {
              "questions": [
                {
                  "questionText": "Nội dung câu hỏi rõ ràng, chính xác?",
                  "answers": [
                    {
                      "answerText": "Đáp án đúng",
                      "isCorrect": true,
                      "explanation": "Giải thích tại sao đúng"
                    },
                    {
                      "answerText": "Đáp án sai 1",
                      "isCorrect": false,
                      "explanation": "Giải thích tại sao sai"
                    },
                    {
                      "answerText": "Đáp án sai 2",
                      "isCorrect": false,
                      "explanation": "Giải thích tại sao sai"
                    },
                    {
                      "answerText": "Đáp án sai 3",
                      "isCorrect": false,
                      "explanation": "Giải thích tại sao sai"
                    }
                  ]
                }
              ]
            }
            
            QUY TẮC BẮT BUỘC:
            - Mỗi câu hỏi có ĐÚNG 4 đáp án
            - CHỈ 1 đáp án có isCorrect: true
            - Câu hỏi phải liên quan đến chủ đề "%s"
            - Đáp án sai phải hợp lý, không quá dễ loại trừ
            - Có explanation cho TẤT CẢ đáp án
            - Ngôn ngữ: Tiếng Việt, phù hợp học sinh
            - Nếu không nói rõ độ khó, tạo câu hỏi trung bình
            - Nếu không nói rõ số lượng, tạo 10 câu
            """.formatted(topic.getName(), topic.getDescription(), userPrompt, topic.getName());
    }
    
    /**
     * Parse JSON từ AI và lưu vào database
     */
    private List<QuestionWithAnswersResponse> parseAndSaveQuestions(String jsonResponse, Long examId) {
        try {
            String cleanJson = jsonResponse
                .replaceAll("```json\\n?", "")
                .replaceAll("```\\n?", "")
                .trim();
            
            AIQuestionResponse aiResponse = objectMapper.readValue(cleanJson, AIQuestionResponse.class);
            
            validateAIResponse(aiResponse);
            
            List<CreateQuestionRequest> questionRequests = aiResponse.getQuestions().stream()
                .map(q -> new CreateQuestionRequest(q.getQuestionText(), examId, "MULTIPLE_CHOICE"))
                .toList();
            
            List<QuestionResponse> savedQuestions = questionService.createBulkQuestions(
                new CreateBulkQuestionsRequest(questionRequests)
            );
            
            for (int i = 0; i < savedQuestions.size(); i++) {
                Long questionId = savedQuestions.get(i).getId();
                List<CreateAnswerRequest> answerRequests = aiResponse.getQuestions().get(i).getAnswers().stream()
                    .map(a -> new CreateAnswerRequest(questionId, a.getAnswerText(), a.getIsCorrect()))
                    .toList();
                
                answerService.createBulkAnswers(new CreateBulkAnswersRequest(answerRequests));
            }
            
            return questionService.getQuestionsByExamId(examId);
            
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            throw new RuntimeException("Không thể tạo câu hỏi. Vui lòng thử lại: " + e.getMessage());
        }
    }
    
    /**
     * Validate response từ AI
     */
    private void validateAIResponse(AIQuestionResponse response) {
        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new RuntimeException("AI không tạo được câu hỏi");
        }
        
        for (var q : response.getQuestions()) {
            if (q.getAnswers() == null || q.getAnswers().size() != 4) {
                throw new RuntimeException("Mỗi câu hỏi phải có đúng 4 đáp án");
            }
            
            long correctCount = q.getAnswers().stream()
                .filter(AIAnswerData::getIsCorrect)
                .count();
            
            if (correctCount != 1) {
                throw new RuntimeException("Mỗi câu hỏi phải có đúng 1 đáp án đúng");
            }
        }
    }
}
