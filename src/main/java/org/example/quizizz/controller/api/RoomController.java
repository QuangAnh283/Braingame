package org.example.quizizz.controller.api;

import org.example.quizizz.common.config.ApiResponse;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.model.dto.room.*;
import org.example.quizizz.security.JwtUtil;
import org.example.quizizz.service.Interface.IRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "8. Phòng chơi", description = "API quản lý phòng chơi, người chơi và lời mời")
public class RoomController {

    private final IRoomService roomService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @Operation(summary = "Tạo phòng mới", description = "Tạo một phòng chơi mới từ đề thi được chọn")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        RoomResponse roomResponse = roomService.createRoom(request, userId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_CREATED, roomResponse));
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "Lấy thông tin phòng theo ID", description = "Trả về thông tin chi tiết phòng theo mã định danh")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long roomId) {
        RoomResponse roomResponse = roomService.getRoomById(roomId);
        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    @GetMapping("/code/{roomCode}")
    @Operation(summary = "Lấy thông tin phòng theo mã phòng", description = "Trả về thông tin phòng thông qua mã phòng")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomByCode(@PathVariable String roomCode) {
        RoomResponse roomResponse = roomService.getRoomByCode(roomCode);
        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    @PutMapping("/{roomId}")
    @Operation(summary = "Cập nhật thông tin phòng", description = "Cập nhật cấu hình phòng chơi bởi chủ phòng")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        RoomResponse roomResponse = roomService.updateRoom(roomId, request, userId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_UPDATED, roomResponse));
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Xóa phòng", description = "Xóa phòng chơi khi chủ phòng kết thúc hoặc hủy phòng")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable Long roomId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        roomService.deleteRoom(roomId, userId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_DELETED, null));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng với phân trang và tìm kiếm", description = "Lấy danh sách phòng đang chờ bắt đầu, hỗ trợ phân trang và tìm kiếm theo tên phòng")
    public ResponseEntity<ApiResponse<PagedRoomResponse>> getRooms(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        PagedRoomResponse rooms = roomService.getAllRoomsSimple(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @PostMapping("/join")
    @Operation(summary = "Tham gia phòng bằng mã phòng", description = "Người chơi tham gia phòng thông qua mã phòng")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        RoomResponse roomResponse = roomService.joinRoom(request, userId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_JOINED, roomResponse));
    }

    @PostMapping("/{roomId}/join-direct")
    @Operation(summary = "Tham gia trực tiếp phòng công khai", description = "Tham gia phòng công khai bằng ID phòng, không cần mã phòng")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoomDirect(
            @PathVariable Long roomId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        RoomResponse roomResponse = roomService.joinRoomById(roomId, userId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_JOINED, roomResponse));
    }

    @DeleteMapping("/{roomId}/leave")
    @Operation(summary = "Rời khỏi phòng", description = "Người chơi rời khỏi phòng hiện tại")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @PathVariable Long roomId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        roomService.leaveRoom(roomId, userId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_LEFT, null));
    }

    @GetMapping("/{roomId}/players")
    @Operation(summary = "Lấy danh sách người chơi trong phòng", description = "Lấy danh sách thành viên đang có mặt trong phòng")
    public ResponseEntity<ApiResponse<List<RoomPlayerResponse>>> getRoomPlayers(@PathVariable Long roomId) {
        List<RoomPlayerResponse> players = roomService.getRoomPlayers(roomId);
        return ResponseEntity.ok(ApiResponse.success(players));
    }

    @DeleteMapping("/{roomId}/kick")
    @Operation(summary = "Mời người chơi rời phòng", description = "Chủ phòng mời một người chơi rời khỏi phòng")
    public ResponseEntity<ApiResponse<Void>> kickPlayer(
            @PathVariable Long roomId,
            @Valid @RequestBody KickPlayerRequest request,
            HttpServletRequest httpRequest) {

        Long hostId = getUserIdFromRequest(httpRequest);
        roomService.kickPlayer(roomId, request, hostId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.PLAYER_KICKED, null));
    }

    @PostMapping("/invite")
    @Operation(summary = "Mời người chơi vào phòng", description = "Gửi lời mời tham gia phòng đến người chơi")
    public ResponseEntity<ApiResponse<InvitationResponse>> invitePlayer(
            @Valid @RequestBody InvitePlayerRequest request,
            HttpServletRequest httpRequest) {

        Long inviterId = getUserIdFromRequest(httpRequest);
        InvitationResponse invitation = roomService.invitePlayer(request, inviterId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, invitation));
    }

    @PostMapping("/invitations/{invitationId}/respond")
    @Operation(summary = "Phản hồi lời mời", description = "Chấp nhận hoặc từ chối lời mời tham gia phòng")
    public ResponseEntity<ApiResponse<RoomResponse>> respondToInvitation(
            @PathVariable Long invitationId,
            @RequestParam boolean accept,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        RoomResponse roomResponse = roomService.respondToInvitation(invitationId, accept, userId);

        if (accept && roomResponse != null) {
            return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_JOINED, roomResponse));
        } else {
            return ResponseEntity.ok(ApiResponse.success(MessageCode.SUCCESS, null));
        }
    }

    @GetMapping("/invitations")
    @Operation(summary = "Lấy danh sách lời mời của người dùng", description = "Lấy toàn bộ lời mời đang chờ phản hồi của người dùng hiện tại")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getUserInvitations(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<InvitationResponse> invitations = roomService.getUserInvitations(userId);
        return ResponseEntity.ok(ApiResponse.success(invitations));
    }

    @PostMapping("/{roomId}/transfer-host")
    @Operation(summary = "Chuyển quyền chủ phòng", description = "Chuyển quyền chủ phòng cho một thành viên khác")
    public ResponseEntity<ApiResponse<RoomResponse>> transferHost(
            @PathVariable Long roomId,
            @RequestParam Long newHostId,
            HttpServletRequest httpRequest) {

        Long currentHostId = getUserIdFromRequest(httpRequest);
        RoomResponse roomResponse = roomService.transferHost(roomId, newHostId, currentHostId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.ROOM_HOST_TRANSFERRED, roomResponse));
    }

    @PostMapping("/{roomId}/start")
    @Operation(summary = "Bắt đầu trò chơi", description = "Chủ phòng bắt đầu ván chơi khi phòng đã sẵn sàng")
    public ResponseEntity<ApiResponse<Void>> startGame(
            @PathVariable Long roomId,
            HttpServletRequest httpRequest) {

        Long hostId = getUserIdFromRequest(httpRequest);
        roomService.startGame(roomId, hostId);

        return ResponseEntity.ok(ApiResponse.success(MessageCode.GAME_STARTED, null));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.getUserIdFromToken(token);
            } catch (Exception e) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED.value(),
                        MessageCode.UNAUTHORIZED,
                        "Invalid JWT token"
                );
            }
        }
        throw new ApiException(
                HttpStatus.UNAUTHORIZED.value(),
                MessageCode.UNAUTHORIZED,
                "No valid JWT token found"
        );
    }
}
