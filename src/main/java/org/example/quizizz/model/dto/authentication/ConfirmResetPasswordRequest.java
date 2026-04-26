package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Yeu cau xac nhan dat lai mat khau bang token")
public class ConfirmResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "Token dat lai mat khau chi dung mot lan", example = "f4f1f21b-...-92")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Mat khau moi", example = "NewP@ssw0rd")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Nhap lai mat khau moi", example = "NewP@ssw0rd")
    private String confirmPassword;
}
