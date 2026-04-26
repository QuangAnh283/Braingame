package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau tham gia phong bang ma phong")
public class JoinRoomRequest {
    @NotBlank(message = "Room code is required")
    @Schema(description = "Ma phong", example = "ABCD12")
    private String roomCode;
}
