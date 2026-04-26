package org.example.quizizz.model.dto.answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau cap nhat dap an")
public class UpdateAnswerRequest {
    @Schema(description = "Noi dung dap an moi", example = "5")
    private String answerText;
    @Schema(description = "Danh dau dap an dung", example = "false")
    private Boolean isCorrect;
}
