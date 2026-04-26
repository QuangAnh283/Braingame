package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau moi nguoi choi vao phong")
public class InvitePlayerRequest {
    @NotNull(message = "Room ID is required")
    @Schema(description = "ID phong", example = "10")
    private Long roomId;

    @NotBlank(message = "Username is required")
    @Schema(description = "Ten dang nhap nguoi duoc moi", example = "player02")
    private String username;

    @Schema(description = "Loi nhan moi (tuy chon)", example = "Vao phong lam bai cung minh nhe")
    private String message;
}
