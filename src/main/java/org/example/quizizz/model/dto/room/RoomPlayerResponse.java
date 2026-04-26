package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thong tin nguoi choi trong phong")
public class RoomPlayerResponse {
    @Schema(description = "ID ban ghi tham gia phong", example = "1001")
    private Long id;
    @Schema(description = "ID nguoi dung", example = "22")
    private Long userId;
    @Schema(description = "Ten dang nhap", example = "player02")
    private String username;
    @Schema(description = "Co phai chu phong hay khong", example = "false")
    private Boolean isHost;
    @Schema(description = "Thoi diem tham gia phong", example = "2026-04-26T09:20:00")
    private LocalDateTime joinedAt;
}
