package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.quizizz.common.constants.RoomMode;
import org.example.quizizz.common.constants.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin chi tiet phong")
public class RoomResponse {
    @Schema(description = "ID phong", example = "10")
    private Long id;
    @Schema(description = "Ma phong", example = "ABCD12")
    private String roomCode;
    @Schema(description = "Ten phong", example = "Phong On Tap Toan 9")
    private String roomName;
    @Schema(description = "Che do phong", example = "CLASSIC")
    private RoomMode roomMode;
    @Schema(description = "ID chu de", example = "3")
    private Long topicId;
    @Schema(description = "Ten chu de", example = "Toan lop 9")
    private String topicName;
    @Schema(description = "ID de thi", example = "12")
    private Long examId;
    @Schema(description = "Tieu de de thi", example = "De on tap hoc ky I")
    private String examTitle;
    @Schema(description = "Danh dau phong rieng tu", example = "false")
    private Boolean isPrivate;
    @Schema(description = "ID chu phong", example = "15")
    private Long ownerId;
    @Schema(description = "Ten dang nhap chu phong", example = "teacher01")
    private String ownerUsername;
    @Schema(description = "Trang thai phong", example = "WAITING")
    private RoomStatus status;
    @Schema(description = "So nguoi choi toi da", example = "20")
    private Integer maxPlayers;
    @Schema(description = "So nguoi choi hien tai", example = "7")
    private Integer currentPlayers;
    @Schema(description = "So cau hoi trong van", example = "10")
    private Integer questionCount;
    @Schema(description = "Thoi gian dem nguoc moi cau hoi (giay)", example = "30")
    private Integer countdownTime;
    @Schema(description = "Thoi diem tao phong", example = "2026-04-26T09:15:30")
    private LocalDateTime createdAt;
}
