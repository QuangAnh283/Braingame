package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ket qua xu ly yeu cau quen mat khau")
public class ResetPasswordResponse {
    @Schema(description = "Thong diep ket qua", example = "Link dat lai mat khau da duoc gui")
    private String message;
    @Schema(description = "Email nhan thong bao", example = "teacher01@example.com")
    private String email;
}
