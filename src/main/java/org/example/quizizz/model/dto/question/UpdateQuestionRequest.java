package org.example.quizizz.model.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau cap nhat cau hoi")
public class UpdateQuestionRequest {
    @Schema(description = "Noi dung cau hoi moi", example = "3 + 5 bang bao nhieu?")
    private String questionText;
    @Schema(description = "ID chu de", example = "3")
    private Long topicId;
    @Schema(description = "Loai cau hoi", example = "MULTIPLE_CHOICE")
    private String questionType;
}
