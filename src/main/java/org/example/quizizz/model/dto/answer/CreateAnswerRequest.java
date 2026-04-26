package org.example.quizizz.model.dto.answer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau tao dap an")
public class CreateAnswerRequest {
    
    @NotNull(message = "Question ID is required")
    @Schema(description = "ID cau hoi", example = "101")
    private Long questionId;
    
    @NotBlank(message = "Answer text is required")
    @Schema(description = "Noi dung dap an", example = "4")
    private String answerText;
    
    @NotNull(message = "Is correct flag is required")
    @Schema(description = "Danh dau dap an dung", example = "true")
    private Boolean isCorrect;
}
