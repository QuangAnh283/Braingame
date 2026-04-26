package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.quizizz.common.constants.RoomMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau tao phong cho game")
public class CreateRoomRequest {
    @NotBlank(message = "Room name is required")
    @Schema(description = "Ten phong", example = "Phong On Tap Toan 9")
    private String roomName;

    @NotNull(message = "Room mode is required")
    @Schema(description = "Che do phong", example = "CLASSIC")
    private RoomMode roomMode;

    @NotNull(message = "Topic ID is required")
    @Schema(description = "ID chu de", example = "3")
    private Long topicId;

    @NotNull(message = "Exam ID is required")
    @Schema(description = "ID de thi", example = "12")
    private Long examId;

    @Schema(description = "Danh dau phong rieng tu", example = "false", defaultValue = "false")
    private Boolean isPrivate = false;
    @Schema(description = "So nguoi choi toi da", example = "20")
    private Integer maxPlayers;
    @Schema(description = "So cau hoi moi van", example = "10", defaultValue = "10")
    private Integer questionCount = 10;
    @Schema(description = "Thoi gian dem nguoc cho moi cau hoi (giay)", example = "30", defaultValue = "30")
    private Integer countdownTime = 30;
}
