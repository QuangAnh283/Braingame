package org.example.quizizz.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.model.dto.leaderboard.LeaderboardEntryResponse;
import org.example.quizizz.service.Interface.ILeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Bảng xếp hạng", description = "API liên quan đến bảng xếp hạng")
public class LeaderboardController {

    private final ILeaderboardService leaderboardService;

    @Operation(summary = "Lấy bảng xếp hạng theo chủ đề", description = "Lấy top 20 người chơi có điểm trung bình cao nhất theo chủ đề")
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboardByTopic(@PathVariable Long topicId) {
        List<LeaderboardEntryResponse> leaderboard = leaderboardService.getLeaderboardByTopic(topicId);
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
    }
}
