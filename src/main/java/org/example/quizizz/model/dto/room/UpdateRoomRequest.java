package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.quizizz.common.constants.RoomMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yeu cau cap nhat thong tin phong")
public class UpdateRoomRequest {
    @Schema(description = "Ten phong moi", example = "Phong On Tap Toan 9 - Ca toi")
    private String roomName;
    @Schema(description = "Che do phong", example = "CLASSIC")
    private RoomMode roomMode;
    @Schema(description = "Danh dau phong rieng tu", example = "true")
    private Boolean isPrivate;
    @Schema(description = "So nguoi choi toi da", example = "30")
    private Integer maxPlayers;
    @Schema(description = "So cau hoi trong van", example = "15")
    private Integer questionCount;
    @Schema(description = "Loai cau hoi duoc phep", example = "MULTIPLE_CHOICE")
    private String questionType;
    @Schema(description = "Thoi gian dem nguoc moi cau hoi (giay)", example = "25")
    private Integer countdownTime;
}
