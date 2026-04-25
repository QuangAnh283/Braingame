package org.example.quizizz.model.dto.socket;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonSetter;

@Data
public class SubmitAnswerSocketRequest {
    private Long roomId;
    private Long questionId;
    private Long answerId;
    private Integer selectedOptionIndex;
    private String selectedAnswer;
    private String answerText;

    @JsonSetter("roomId")
    public void setRoomId(Object roomId) {
        if (roomId instanceof Integer) {
            this.roomId = ((Integer) roomId).longValue();
        } else if (roomId instanceof Long) {
            this.roomId = (Long) roomId;
        } else if (roomId instanceof Number) {
            this.roomId = ((Number) roomId).longValue();
        } else if (roomId instanceof String) {
            this.roomId = Long.parseLong((String) roomId);
        }
    }

    @JsonSetter("questionId")
    public void setQuestionId(Object questionId) {
        if (questionId instanceof Integer) {
            this.questionId = ((Integer) questionId).longValue();
        } else if (questionId instanceof Long) {
            this.questionId = (Long) questionId;
        } else if (questionId instanceof Number) {
            this.questionId = ((Number) questionId).longValue();
        } else if (questionId instanceof String) {
            this.questionId = Long.parseLong((String) questionId);
        }
    }

    @JsonSetter("answerId")
    public void setAnswerId(Object answerId) {
        if (answerId instanceof Integer) {
            this.answerId = ((Integer) answerId).longValue();
        } else if (answerId instanceof Long) {
            this.answerId = (Long) answerId;
        } else if (answerId instanceof Number) {
            this.answerId = ((Number) answerId).longValue();
        } else if (answerId instanceof String) {
            this.answerId = Long.parseLong((String) answerId);
        }
    }

}
