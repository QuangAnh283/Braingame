package org.example.quizizz.model.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau tao cau hoi")
public class CreateQuestionRequest {

    @NotBlank(message = "Question text is required")
    @Schema(description = "Noi dung cau hoi", example = "2 + 2 bang bao nhieu?")
    private String questionText;

    @NotNull(message = "Exam ID is required")
    @Schema(description = "ID de thi chua cau hoi", example = "12")
    private Long examId;

    @NotNull(message = "Question type is required")
    @Schema(description = "Loai cau hoi", example = "MULTIPLE_CHOICE")
    private String questionType;
}
