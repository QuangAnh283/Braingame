package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin tra ve sau khi dang nhap thanh cong")
public class LoginResponse {
    @Schema(description = "ID nguoi dung", example = "15")
    private Long userId;
    @Schema(description = "Access token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;
    @Schema(description = "Refresh token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
    @Schema(description = "Ten dang nhap", example = "teacher01")
    private String username;
    @Schema(description = "Ho va ten", example = "Nguyen Van A")
    private String fullName;
    @Schema(description = "Loai tai khoan", example = "TEACHER")
    private String typeAccount;
}
