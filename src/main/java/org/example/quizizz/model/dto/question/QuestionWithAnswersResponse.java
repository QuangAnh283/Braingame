package org.example.quizizz.model.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.quizizz.model.dto.answer.AnswerResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin cau hoi kem danh sach dap an")
public class QuestionWithAnswersResponse {
    @Schema(description = "ID cau hoi", example = "101")
    private Long id;
    @Schema(description = "Noi dung cau hoi", example = "2 + 2 bang bao nhieu?")
    private String questionText;
    @Schema(description = "ID de thi", example = "12")
    private Long examId;
    @Schema(description = "Loai cau hoi", example = "MULTIPLE_CHOICE")
    private String questionType;
    @Schema(description = "Danh sach dap an cua cau hoi")
    private List<AnswerResponse> answers;
}
