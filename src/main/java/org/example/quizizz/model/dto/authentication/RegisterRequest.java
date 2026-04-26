package org.example.quizizz.model.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Yeu cau dang ky tai khoan moi")
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "Ten dang nhap duy nhat", example = "teacher01")
    private String username;

    @NotBlank(message = "Full name is required")
    @Schema(description = "Ho va ten", example = "Nguyen Van A")
    private String fullName;

    @Email(message = "Email is invalid")
    @NotBlank(message = "Email is required")
    @Schema(description = "Dia chi email", example = "teacher01@example.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "So dien thoai", example = "0901234567")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Schema(description = "Dia chi lien he", example = "123 Nguyen Trai, Ha Noi")
    private String address;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "Mat khau tai khoan", example = "P@ssw0rd")
    private String password;

    @NotNull(message = "Date of birth is required")
    @Schema(description = "Ngay sinh theo dinh dang yyyy-MM-dd", example = "2000-01-01")
    private LocalDate dob;
}
