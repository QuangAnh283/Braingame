package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Yeu cau khoi tao quy trinh quen mat khau")
public class ResetPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email dang ky tai khoan", example = "teacher01@example.com")
    private String email;
}
