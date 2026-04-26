package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau dang nhap vao he thong")
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Ten dang nhap", example = "teacher01")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Mat khau dang nhap", example = "P@ssw0rd")
    private String password;
}
