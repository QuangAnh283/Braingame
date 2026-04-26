package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin loi moi vao phong")
public class InvitationResponse {
    @Schema(description = "ID loi moi", example = "501")
    private Long id;
    @Schema(description = "ID phong", example = "10")
    private Long roomId;
    @Schema(description = "Ten phong", example = "Phong On Tap Toan 9")
    private String roomName;
    @Schema(description = "ID nguoi moi", example = "15")
    private Long inviterId;
    @Schema(description = "Ten dang nhap nguoi moi", example = "teacher01")
    private String inviterUsername;
    @Schema(description = "ID nguoi duoc moi", example = "22")
    private Long inviteeId;
    @Schema(description = "Ten dang nhap nguoi duoc moi", example = "player02")
    private String inviteeUsername;
    @Schema(description = "Trang thai loi moi", example = "PENDING")
    private String status; // PENDING, ACCEPTED, DECLINED
    @Schema(description = "Thoi diem tao loi moi", example = "2026-04-26T09:25:00")
    private LocalDateTime createdAt;
    @Schema(description = "Thoi diem het han loi moi", example = "2026-04-26T09:55:00")
    private LocalDateTime expiresAt;
}
