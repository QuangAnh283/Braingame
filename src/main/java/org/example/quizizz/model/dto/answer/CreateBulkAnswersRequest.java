package org.example.quizizz.model.dto.answer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau tao nhieu dap an cung luc")
public class CreateBulkAnswersRequest {
    @NotEmpty(message = "Answers list cannot be empty")
    @Valid
    @Schema(description = "Danh sach dap an can tao")
    private List<CreateAnswerRequest> answers;
}