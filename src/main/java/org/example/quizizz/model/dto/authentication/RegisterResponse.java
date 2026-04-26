package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Thong tin tai khoan sau khi dang ky thanh cong")
public class RegisterResponse {
    @Schema(description = "ID nguoi dung", example = "15")
    private Long userId;
    @Schema(description = "Ten dang nhap", example = "teacher01")
    private String username;
    @Schema(description = "Ho va ten", example = "Nguyen Van A")
    private String fullName;
    @Schema(description = "Email da dang ky", example = "teacher01@example.com")
    private String email;
}
