package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau moi chu phong kick nguoi choi")
public class KickPlayerRequest {
    @NotNull(message = "Player ID is required")
    @Schema(description = "ID nguoi choi bi moi", example = "88")
    private Long playerId;

    @Schema(description = "Ly do moi (tuy chon)", example = "Vi pham noi quy phong")
    private String reason;
}
