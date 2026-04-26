package org.example.quizizz.controller.socketio.listener;

import com.corundumstudio.socketio.SocketIOServer;
import org.example.quizizz.controller.socketio.broadcast.GameSocketFacade;
import org.example.quizizz.controller.socketio.session.SessionManager;
import org.example.quizizz.model.dto.game.*;
import org.example.quizizz.model.dto.socket.NextQuestionRequest;
import org.example.quizizz.model.dto.socket.StartGameRequest;
import org.example.quizizz.model.dto.socket.SubmitAnswerSocketRequest;
import org.example.quizizz.model.entity.Room;
import org.example.quizizz.repository.RoomRepository;
import org.example.quizizz.service.Interface.IGameService;
import org.example.quizizz.service.Interface.IRoomService;
import org.example.quizizz.service.helper.GameTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameEventHandler {

    private final IGameService gameService;
    private final IRoomService roomService;
    private final SessionManager sessionManager;
    private final RoomRepository roomRepository;
    private final GameTimerService gameTimerService;
    private final GameSocketFacade gameSocketFacade;

    /*** Đăng ký các sự kiện game ***/
    public void registerEvents(SocketIOServer server) {

        /*** Bắt đầu game - chỉ host mới được phép ***/
        server.addEventListener("start-game", StartGameRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", false,
                                "message", "User not authenticated"
                        ));
                    }
                    client.sendEvent("error", Map.of("message", "User not authenticated"));
                    return;
                }

                if (!roomService.isRoomHost(data.getRoomId(), userId)) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", false,
                                "message", "Only host can start game"
                        ));
                    }
                    client.sendEvent("error", Map.of("message", "Only host can start game"));
                    return;
                }

                log.info("User {} starting game in room {}", userId, data.getRoomId());

                // Bắt đầu game session
                gameService.startGameSession(data.getRoomId());
                roomService.startGame(data.getRoomId(), userId);

                Room room = roomRepository.findById(data.getRoomId()).orElseThrow();

                // Lấy câu hỏi đầu tiên
                NextQuestionResponse firstQuestion = gameService.getNextQuestion(data.getRoomId());

                if (firstQuestion != null) {
                    log.info("First question loaded: {}", firstQuestion.getQuestionText());

                    // Broadcast game bắt đầu đến tất cả players
                    gameSocketFacade.notifyGameStarted(room.getRoomCode(), data.getRoomId(), firstQuestion);

                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", true,
                                "roomId", data.getRoomId(),
                                "question", firstQuestion
                        ));
                    }

                    // Bắt đầu đếm ngược server-authoritative theo roomCode
                    gameTimerService.startGameTimer(data.getRoomId(), room.getRoomCode(), firstQuestion.getTimeLimit());
                } else {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", false,
                                "message", "No questions available"
                        ));
                    }
                    client.sendEvent("error", Map.of("message", "No questions available"));
                }

            } catch (Exception e) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData(Map.of(
                            "success", false,
                            "message", "Failed to start game: " + e.getMessage()
                    ));
                }
                client.sendEvent("error", Map.of("message", "Failed to start game: " + e.getMessage()));
            }
        });

        /*** Gửi đáp án ***/
        server.addEventListener("submit-answer", SubmitAnswerSocketRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null) {
                    client.sendEvent("error", Map.of("message", "User not authenticated"));
                    return;
                }

                log.info("📝 User {} submitting answer for question {} in room {}",
                    userId, data.getQuestionId(), data.getRoomId());

                Long answerId = data.getAnswerId() != null
                        ? data.getAnswerId()
                        : gameService.resolveAnswerId(
                                data.getQuestionId(),
                                data.getSelectedOptionIndex(),
                                data.getSelectedAnswer(),
                                data.getAnswerText()
                        );

                AnswerSubmitRequest request = new AnswerSubmitRequest();
                request.setQuestionId(data.getQuestionId());
                request.setAnswerId(answerId);

                // Gửi đáp án và nhận kết quả
                QuestionResultResponse result = gameService.submitAnswer(data.getRoomId(), userId, request);

                log.info("✅ Answer result for user {}: isCorrect={}, score={}, streak={}",
                    userId, result.getIsCorrect(), result.getScore(), result.getStreak());

                client.sendEvent("answer-submitted", Map.of(
                        "result", result,
                        "hasNextQuestion", false,
                        "timestamp", System.currentTimeMillis()
                ));

                boolean allCompleted = gameService.haveAllPlayersCompleted(data.getRoomId());
                log.info("🎯 Room {}: All players completed? {}", data.getRoomId(), allCompleted);

                if (allCompleted) {
                    log.info("🎉 All players completed! Ending game for room {}", data.getRoomId());

                    // Dừng timer hiện tại để tránh kết thúc game trùng do time-up event.
                    gameTimerService.stopGameTimer(data.getRoomId());

                    // Tính toán kết quả cuối cùng
                    GameOverResponse gameResult = gameService.endGame(data.getRoomId());

                    Room room = roomRepository.findById(data.getRoomId()).orElseThrow();
                    // Broadcast kết quả đến tất cả players
                    gameSocketFacade.notifyGameFinished(room.getRoomCode(), gameResult);
                }

                Room room = roomRepository.findById(data.getRoomId()).orElseThrow();
                // Broadcast rằng user đã trả lời (không tiết lộ đáp án)
                    gameSocketFacade.notifyPlayerAnswered(room.getRoomCode(), userId);

            } catch (Exception e) {
                log.error("❌ Error submitting answer for user: {}", e.getMessage(), e);
                client.sendEvent("error", Map.of("message", "Failed to submit answer: " + e.getMessage()));
            }
        });

        /*** Hiển thị kết quả câu hỏi - chỉ host ***/
        server.addEventListener("show-question-result", NextQuestionRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null || !roomService.isRoomHost(data.getRoomId(), userId)) {
                    client.sendEvent("error", Map.of("message", "Only host can control game"));
                    return;
                }

                Room room = roomRepository.findById(data.getRoomId()).orElseThrow();
                // Broadcast kết quả câu hỏi đến tất cả players
                gameSocketFacade.notifyQuestionResultShown(room.getRoomCode(), data.getRoomId());

            } catch (Exception e) {
                log.error("Error showing question result: {}", e.getMessage());
            }
        });

        /*** Chuyển câu hỏi tiếp theo - chỉ host ***/
        server.addEventListener("next-question", NextQuestionRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null || !roomService.isRoomHost(data.getRoomId(), userId)) {
                    client.sendEvent("error", Map.of("message", "Only host can control game"));
                    return;
                }

                Long roomId = data.getRoomId();
                NextQuestionResponse nextQuestion = gameService.getNextQuestion(roomId);
                Room room = roomRepository.findById(roomId).orElseThrow();

                if (nextQuestion != null) {
                    // Broadcast câu hỏi tiếp theo
                    gameSocketFacade.notifyNextQuestion(room.getRoomCode(), nextQuestion);

                    // Bắt đầu đếm ngược mới
                    gameTimerService.startGameTimer(roomId, room.getRoomCode(), nextQuestion.getTimeLimit());
                } else {
                    gameTimerService.stopGameTimer(roomId);

                    // Kết thúc game
                    GameOverResponse gameResult = gameService.endGame(roomId);
                        gameSocketFacade.notifyGameFinished(room.getRoomCode(), gameResult);
                }

            } catch (Exception e) {
                log.error("Error getting next question: {}", e.getMessage());
                client.sendEvent("error", Map.of("message", "Failed to get next question"));
            }
        });

        /*** Kết thúc game sớm - chỉ host ***/
        server.addEventListener("end-game", StartGameRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null || !roomService.isRoomHost(data.getRoomId(), userId)) {
                    client.sendEvent("error", Map.of("message", "Only host can end game"));
                    return;
                }

                // Dừng timer
                gameTimerService.stopGameTimer(data.getRoomId());

                // Kết thúc game
                GameOverResponse gameResult = gameService.endGame(data.getRoomId());
                Room room = roomRepository.findById(data.getRoomId()).orElseThrow();
                gameSocketFacade.notifyGameFinished(room.getRoomCode(), gameResult);

            } catch (Exception e) {
                log.error("Error ending game: {}", e.getMessage());
                client.sendEvent("error", Map.of("message", "Failed to end game"));
            }
        });

        server.addEventListener("get-game-state", StartGameRequest.class, (client, data, ackRequest) -> {
            try {
                Long userId = sessionManager.getUserId(client.getSessionId());
                if (userId == null) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", false,
                                "message", "User not authenticated"
                        ));
                    }
                    return;
                }

                Long roomId = data.getRoomId();
                if (!roomService.isUserInRoom(roomId, userId)) {
                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", false,
                                "message", "Permission denied"
                        ));
                    }
                    return;
                }

                // Lấy câu hỏi hiện tại
                NextQuestionResponse currentQuestion = gameService.getCurrentQuestion(roomId);

                if (currentQuestion != null) {
                    Integer remainingTime = gameTimerService.getRemainingTime(roomId);

                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", true,
                                "currentQuestion", currentQuestion,
                                "remainingTime", remainingTime != null ? remainingTime : currentQuestion.getTimeLimit(),
                                "timestamp", System.currentTimeMillis()
                        ));
                    }
                } else {
                    log.warn("⚠️ No current question found for room {}", roomId);

                    if (ackRequest.isAckRequested()) {
                        ackRequest.sendAckData(Map.of(
                                "success", false,
                                "message", "No current question available"
                        ));
                    }
                }

            } catch (Exception e) {
                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData(Map.of(
                            "success", false,
                            "message", "Failed to get game state: " + e.getMessage()
                    ));
                }
            }
        });
    }
}
