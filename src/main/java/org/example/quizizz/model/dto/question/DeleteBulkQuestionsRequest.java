package org.example.quizizz.model.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau xoa nhieu cau hoi")
public class DeleteBulkQuestionsRequest {
    
    @NotEmpty(message = "Question IDs list cannot be empty")
    @Schema(description = "Danh sach ID cau hoi can xoa", example = "[101,102,103]")
    private List<Long> questionIds;
}
