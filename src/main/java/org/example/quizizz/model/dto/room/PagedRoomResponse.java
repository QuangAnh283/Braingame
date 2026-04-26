package org.example.quizizz.model.dto.room;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Du lieu phong co phan trang")
public class PagedRoomResponse {
    @Schema(description = "Danh sach phong trong trang hien tai")
    private List<RoomResponse> rooms;
    @Schema(description = "Trang hien tai bat dau tu 0", example = "0")
    private int currentPage;
    @Schema(description = "Tong so trang", example = "5")
    private int totalPages;
    @Schema(description = "Tong so ban ghi", example = "92")
    private long totalElements;
    @Schema(description = "Kich thuoc trang", example = "10")
    private int pageSize;
    @Schema(description = "Con trang tiep theo hay khong", example = "true")
    private boolean hasNext;
    @Schema(description = "Co trang truoc do hay khong", example = "false")
    private boolean hasPrevious;
}
