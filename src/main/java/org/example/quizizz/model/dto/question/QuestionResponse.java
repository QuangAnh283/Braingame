package org.example.quizizz.model.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin cau hoi")
public class QuestionResponse {
    @Schema(description = "ID cau hoi", example = "101")
    private Long id;
    @Schema(description = "Noi dung cau hoi", example = "2 + 2 bang bao nhieu?")
    private String questionText;
    @Schema(description = "ID chu de", example = "3")
    private Long topicId;
    @Schema(description = "Loai cau hoi", example = "MULTIPLE_CHOICE")
    private String questionType;
    @Schema(description = "Thoi diem tao", example = "2026-04-26T09:30:00")
    private LocalDateTime createdAt;
    @Schema(description = "Thoi diem cap nhat", example = "2026-04-26T10:00:00")
    private LocalDateTime updatedAt;
}
