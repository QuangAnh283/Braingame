package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Yeu cau doi mat khau cho nguoi dung hien tai")
public class ChangePasswordRequest {
    
    @NotBlank(message = "Current password is required")
    @Schema(description = "Mat khau hien tai", example = "OldP@ssw0rd")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 50, message = "New password must be between 6 and 50 characters")
    @Schema(description = "Mat khau moi", example = "NewP@ssw0rd")
    private String newPassword;
    
    @NotBlank(message = "Confirm password is required")
    @Schema(description = "Nhap lai mat khau moi", example = "NewP@ssw0rd")
    private String confirmPassword;
}
