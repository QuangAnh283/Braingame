package org.example.quizizz.model.dto.question;

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
@Schema(description = "Yeu cau tao nhieu cau hoi cung luc")
public class CreateBulkQuestionsRequest {
    
    @NotEmpty(message = "Questions list cannot be empty")
    @Valid
    @Schema(description = "Danh sach cau hoi can tao")
    private List<CreateQuestionRequest> questions;
}
