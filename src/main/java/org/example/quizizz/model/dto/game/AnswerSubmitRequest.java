package org.example.quizizz.model.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitRequest {
    private Long questionId;
    private Long answerId;
    /** Thời gian player dùng để trả lời câu hỏi này (ms). Frontend đo từ lúc nhận câu hỏi tới khi submit. */
    private Long timeTaken;
}
