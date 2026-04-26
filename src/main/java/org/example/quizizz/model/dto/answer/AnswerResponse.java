package org.example.quizizz.model.dto.answer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin dap an")
public class AnswerResponse {
    @Schema(description = "ID dap an", example = "2001")
    private Long id;
    @Schema(description = "ID cau hoi", example = "101")
    private Long questionId;
    @Schema(description = "Noi dung dap an", example = "4")
    private String answerText;
    @Schema(description = "Danh dau dap an dung", example = "true")
    private Boolean isCorrect;
}
