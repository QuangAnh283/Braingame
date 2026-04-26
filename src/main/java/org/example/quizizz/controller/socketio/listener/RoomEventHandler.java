package org.example.quizizz.controller.socketio.listener;

import com.corundumstudio.socketio.SocketIOServer;
import org.example.quizizz.controller.socketio.broadcast.RoomSocketFacade;
import org.example.quizizz.controller.socketio.session.SessionManager;
import org.example.quizizz.model.dto.room.CreateRoomRequest;
import org.example.quizizz.model.dto.room.JoinRoomRequest;
import org.example.quizizz.model.dto.room.RoomPlayerResponse;
import org.example.quizizz.model.dto.room.RoomResponse;
import org.example.quizizz.model.dto.socket.CreateRoomSocketRequest;
import org.example.quizizz.model.dto.socket.GetPlayersRequest;
import org.example.quizizz.model.dto.socket.JoinRoomSocketRequest;
import org.example.quizizz.model.dto.socket.LeaveRoomSocketRequest;
import org.example.quizizz.model.entity.Room;
import org.example.quizizz.repository.RoomRepository;
import org.example.quizizz.service.Interface.IRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomEventHandler {
    
    private final IRoomService roomService;
    private final SessionManager sessionManager;
    private final RoomRepository roomRepository;
    private final RoomSocketFacade roomSocketFacade;
    
    public void registerEvents(SocketIOServer server) {
        
        // Xử lý tạo phòng mới
        server.addEventListener("create-room", CreateRoomSocketRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null) {
                    client.sendEvent("error", Map.of("message", "Unauthorized"));
                    return;
                }
                
                // Convert socket request to room request
                CreateRoomRequest createRoomRequest = new CreateRoomRequest(
                    data.getRoomName(),
                    data.getRoomMode(),
                    data.getTopicId(),
                    data.getExamId(),
                    data.getIsPrivate(),
                    data.getMaxPlayers(),
                    data.getQuestionCount(),
                    data.getCountdownTime()
                );
                
                // Tạo phòng mới
                RoomResponse room = roomService.createRoom(createRoomRequest, userId);
                
                // Tham gia vào phòng socket
                client.joinRoom("room-" + room.getRoomCode());
                
                // Thêm session
                sessionManager.addRoomSession(client.getSessionId().toString(), room.getId());
                
                // Lấy danh sách người chơi
                List<RoomPlayerResponse> players = roomService.getRoomPlayers(room.getId());
                
                // Gửi sự kiện cho client tạo phòng thành công
                client.sendEvent("room-created-success", Map.of(
                    "room", room,
                    "players", players
                ));
                
                log.info("User {} created room {}", userId, room.getId());
                
            } catch (Exception e) {
                log.error("Error creating room: {}", e.getMessage());
                client.sendEvent("error", Map.of("message", e.getMessage()));
            }
        });
        
        /*** Tham gia phòng ***/
        server.addEventListener("join-room", JoinRoomSocketRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null) {
                    client.sendEvent("join-room-error", Map.of("message", "User not authenticated"));
                    return;
                }

                // Join room thông qua service theo roomCode để hỗ trợ cả phòng public/private.
                RoomResponse room = roomService.joinRoom(new JoinRoomRequest(data.getRoomCode()), userId);
                
                // Join socket room
                client.joinRoom("room-" + data.getRoomCode());
                sessionManager.addRoomSession(client.getSessionId().toString(), room.getId());
                
                // Lấy danh sách players sau khi join
                List<RoomPlayerResponse> players = roomService.getRoomPlayers(room.getId());
                RoomPlayerResponse newPlayer = players.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);

                // Gửi thông báo đến tất cả clients trong phòng
                roomSocketFacade.notifyPlayerJoined(
                    data.getRoomCode(),
                    room.getId(),
                    newPlayer != null ? newPlayer : Map.of("userId", userId),
                    players.size()
                );
                
                // Cập nhật danh sách players cho tất cả
                roomSocketFacade.notifyRoomPlayers(data.getRoomCode(), room.getId(), players);
                
                client.sendEvent("join-room-success", Map.of(
                    "success", true,
                    "room", room,
                    "players", players,
                    "timestamp", System.currentTimeMillis()
                ));

            } catch (Exception e) {

                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Failed to join room";
                }

                client.sendEvent("join-room-error", Map.of(
                    "message", errorMessage,
                    "error", errorMessage
                ));
            }
        });
        
        /*** Rời phòng ***/
        server.addEventListener("leave-room", LeaveRoomSocketRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null) return;
                
                Room room = roomRepository.findById(data.getRoomId()).orElse(null);
                if (room == null) return;
                
                // Lấy thông tin player trước khi rời
                List<RoomPlayerResponse> playersBefore = roomService.getRoomPlayers(data.getRoomId());
                RoomPlayerResponse leavingPlayer = playersBefore.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);
                
                String roomCode = room.getRoomCode();
                
                // Leave socket room
                client.leaveRoom("room-" + roomCode);
                sessionManager.removeRoomSession(client.getSessionId().toString());
                
                // Rời phòng thông qua service
                roomService.leaveRoom(data.getRoomId(), userId);
                
                // Gửi thông báo cho client rời phòng
                client.sendEvent("leave-room-success", Map.of(
                    "roomId", data.getRoomId(),
                    "timestamp", System.currentTimeMillis()
                ));
                
                try {
                    // Lấy danh sách players sau khi rời
                    List<RoomPlayerResponse> playersAfter = roomService.getRoomPlayers(data.getRoomId());
                    
                    // Thông báo player đã rời
                    roomSocketFacade.notifyPlayerLeft(roomCode, data.getRoomId(), leavingPlayer, playersAfter.size());
                    
                    // Cập nhật danh sách players
                    roomSocketFacade.notifyRoomPlayers(roomCode, data.getRoomId(), playersAfter);
                } catch (Exception ignored) {
                    // Phòng có thể đã bị xóa nếu trống
                }
                
                log.info("User {} left room {}", userId, data.getRoomId());
                
            } catch (Exception e) {
                log.error("Error leaving room: {}", e.getMessage());
                client.sendEvent("leave-room-error", Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Failed to leave room"
                ));
            }
        });
        
        server.addEventListener("get-players", GetPlayersRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null || !roomService.isUserInRoom(data.getRoomId(), userId)) {
                    client.sendEvent("error", Map.of("message", "Permission denied"));
                    return;
                }
                List<RoomPlayerResponse> players = roomService.getRoomPlayers(data.getRoomId());
                client.sendEvent("room-players", Map.of(
                    "roomId", data.getRoomId(),
                    "players", players
                ));
            } catch (Exception e) {
                client.sendEvent("error", Map.of("message", e.getMessage()));
            }
        });
    }
}
