package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Ket qua doi mat khau")
public class ChangePasswordResponse {
    @Schema(description = "Thong diep ket qua", example = "Doi mat khau thanh cong")
    private String message;
    @Schema(description = "Ten dang nhap cua tai khoan", example = "teacher01")
    private String username;
}
