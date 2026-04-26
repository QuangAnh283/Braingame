package org.example.quizizz.service.Implement;

import org.example.quizizz.common.constants.GameStatus;
import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.constants.RoomStatus;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.controller.socketio.broadcast.GameSocketFacade;
import org.example.quizizz.model.dto.game.*;
import org.example.quizizz.model.dto.game.AnswerOption;
import org.example.quizizz.model.dto.game.PlayerRanking;
import org.example.quizizz.model.dto.game.PlayerScore;
import org.example.quizizz.model.entity.*;
import org.example.quizizz.repository.*;
import org.example.quizizz.service.Interface.IGameService;
import org.example.quizizz.service.Interface.IRedisService;
import org.example.quizizz.service.helper.GameAsyncPostProcessor;
import org.example.quizizz.service.helper.GameScoreCalculator;
import org.example.quizizz.service.helper.GameTimerEvent;
import org.example.quizizz.service.helper.GameTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameServiceImplement implements IGameService {

    private final IRedisService redisService;
    private final GameSessionRepository gameSessionRepository;
    private final GameQuestionRepository gameQuestionRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final GameHistoryRepository gameHistoryRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final GameScoreCalculator scoreCalculator;
    private final GameAsyncPostProcessor gameAsyncPostProcessor;
    private final UserRepository userRepository;
    private final GameSocketFacade gameSocketFacade;
    private final GameTimerService gameTimerService;
    private final Map<Long, Object> roomLocks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void startGameSession(Long roomId) {
        // Lấy room trước để lấy thông tin
        Room room = roomRepository.findById(roomId).orElseThrow();

        // Tạo game session mới
        GameSession gameSession = new GameSession();
        gameSession.setRoomId(roomId);
        gameSession.setRoomStatus(room.getStatus());
        gameSession.setGameStatus("IN_PROGRESS");
        gameSession.setStartTime(LocalDateTime.now());
        gameSession = gameSessionRepository.save(gameSession);

        // Lấy danh sách câu hỏi từ exam của room
        List<Question> questions = questionRepository.findByExamId(room.getExamId());

        // Shuffle và lấy số lượng theo cấu hình phòng
        Collections.shuffle(questions);
        int questionCount = room.getQuestionCount();
        questions = questions.subList(0, Math.min(questionCount, questions.size()));

        // Tạo game questions
        for (int i = 0; i < questions.size(); i++) {
            GameQuestion gameQuestion = new GameQuestion();
            gameQuestion.setGameSessionId(gameSession.getId());
            gameQuestion.setQuestionId(questions.get(i).getId());
            gameQuestion.setQuestionOrder(i);
            gameQuestion.setTimeLimit(Duration.ofSeconds(room.getCountdownTime()));
            gameQuestionRepository.save(gameQuestion);
        }

        // Lưu vào Redis
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("gameSessionId", gameSession.getId());
        sessionData.put("currentQuestionIndex", 0);
        sessionData.put("totalQuestions", questions.size());
        sessionData.put("status", GameStatus.IN_PROGRESS.name());
        redisService.saveGameSession("game:" + roomId, sessionData);

        log.info("Started game session {} for room {} with {} questions", gameSession.getId(), roomId, questions.size());
    }

    @Override
    public NextQuestionResponse getNextQuestion(Long roomId) {
        String gameId = "game:" + roomId;
        Map<String, Object> sessionData = redisService.getGameSession(gameId);

        if (sessionData == null || !GameStatus.IN_PROGRESS.name().equals(sessionData.get("status"))) {
            throw new RuntimeException("Game not active");
        }

        int currentIndex = ((Number) sessionData.get("currentQuestionIndex")).intValue();
        int totalQuestions = ((Number) sessionData.get("totalQuestions")).intValue();
        Long gameSessionId = ((Number) sessionData.get("gameSessionId")).longValue();

        if (currentIndex >= totalQuestions) {
            // Game over
            endGame(roomId);
            return null;
        }

        // Lấy thông tin phòng và câu hỏi tiếp theo
        Room room = roomRepository.findById(roomId).orElseThrow();
        List<GameQuestion> gameQuestions = gameQuestionRepository
                .findByGameSessionIdOrderByQuestionOrder(gameSessionId);
        GameQuestion currentGameQuestion = gameQuestions.get(currentIndex);
        Question question = questionRepository.findById(currentGameQuestion.getQuestionId()).orElseThrow();

        // Lấy đáp án và shuffle để tráo vị trí
        List<Answer> answers = answerRepository.findByQuestionId(question.getId());
        Collections.shuffle(answers); // Tráo đáp án để đáp án đúng không luôn ở vị trí A
        List<AnswerOption> answerOptions = answers.stream()
                .map(a -> new AnswerOption(a.getId(), a.getAnswerText()))
                .collect(Collectors.toList());

        NextQuestionResponse response = new NextQuestionResponse();
        response.setQuestionId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setAnswers(answerOptions);
        response.setTimeLimit(room.getCountdownTime());
        response.setQuestionNumber(currentIndex + 1);
        response.setTotalQuestions(totalQuestions);

        long startedAt = System.currentTimeMillis();
        long expiresAt = startedAt + room.getCountdownTime() * 1000L;

        // Cập nhật current index và state câu hỏi server-authoritative.
        redisService.updateGameSession(gameId, "currentQuestionIndex", currentIndex + 1);
        redisService.updateGameSession(gameId, "currentQuestionId", question.getId());
        redisService.updateGameSession(gameId, "currentQuestionStartedAt", startedAt);
        redisService.updateGameSession(gameId, "currentQuestionExpiresAt", expiresAt);
        redisService.updateGameSession(gameId, "questionVersion", currentIndex + 1);

        return response;
    }

    @Override
    public NextQuestionResponse getCurrentQuestion(Long roomId) {
        String gameId = "game:" + roomId;
        Map<String, Object> sessionData = redisService.getGameSession(gameId);

        if (sessionData == null || !GameStatus.IN_PROGRESS.name().equals(sessionData.get("status"))) {
            log.warn("Game not active for room {}", roomId);
            return null;
        }

        int currentIndex = ((Number) sessionData.get("currentQuestionIndex")).intValue();
        int totalQuestions = ((Number) sessionData.get("totalQuestions")).intValue();
        Long gameSessionId = ((Number) sessionData.get("gameSessionId")).longValue();

        if (currentIndex <= 0) {
            log.warn("No question shown yet for room {}", roomId);
            return null;
        }

        int actualQuestionIndex = currentIndex - 1;

        if (actualQuestionIndex < 0 || actualQuestionIndex >= totalQuestions) {
            log.warn("No current question available for room {}, index: {}", roomId, actualQuestionIndex);
            return null;
        }

        // Lấy thông tin phòng và câu hỏi hiện tại
        Room room = roomRepository.findById(roomId).orElseThrow();
        List<GameQuestion> gameQuestions = gameQuestionRepository
                .findByGameSessionIdOrderByQuestionOrder(gameSessionId);
        GameQuestion currentGameQuestion = gameQuestions.get(actualQuestionIndex);
        Question question = questionRepository.findById(currentGameQuestion.getQuestionId()).orElseThrow();

        // Lấy đáp án
        List<Answer> answers = answerRepository.findByQuestionId(question.getId());
        List<AnswerOption> answerOptions = answers.stream()
                .map(a -> new AnswerOption(a.getId(), a.getAnswerText()))
                .collect(Collectors.toList());

        NextQuestionResponse response = new NextQuestionResponse();
        response.setQuestionId(question.getId());
        response.setQuestionText(question.getQuestionText());
        response.setAnswers(answerOptions);
        response.setTimeLimit(room.getCountdownTime());
        response.setQuestionNumber(actualQuestionIndex + 1);
        response.setTotalQuestions(totalQuestions);

        log.info("Returned current question {} for room {}", actualQuestionIndex + 1, roomId);

        return response;
    }

    @Override
    @Transactional
    public QuestionResultResponse submitAnswer(Long roomId, Long userId, AnswerSubmitRequest request) {
        if (roomId == null || userId == null || request == null || request.getQuestionId() == null || request.getAnswerId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.BAD_REQUEST, "Missing answer submission data");
        }

        if (!roomPlayerRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), MessageCode.ROOM_PERMISSION_DENIED, "Player is not active in this room");
        }

        String gameId = "game:" + roomId;
        Map<String, Object> sessionData = redisService.getGameSession(gameId);
        if (sessionData == null || !GameStatus.IN_PROGRESS.name().equals(sessionData.get("status"))) {
            throw new ApiException(HttpStatus.CONFLICT.value(), MessageCode.GAME_ALREADY_STARTED, "Game is not active");
        }

        Long currentQuestionId = asLong(sessionData.get("currentQuestionId"));
        Long startedAt = asLong(sessionData.get("currentQuestionStartedAt"));
        Long expiresAt = asLong(sessionData.get("currentQuestionExpiresAt"));
        long now = System.currentTimeMillis();

        if (currentQuestionId == null || startedAt == null || expiresAt == null) {
            throw new ApiException(HttpStatus.CONFLICT.value(), MessageCode.BAD_REQUEST, "No active question is available");
        }
        if (!currentQuestionId.equals(request.getQuestionId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.BAD_REQUEST, "Answer is not for the active question");
        }
        if (now > expiresAt) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.BAD_REQUEST, "Time is up for this question");
        }
        if (userAnswerRepository.existsByRoomIdAndUserIdAndQuestionId(roomId, userId, request.getQuestionId())) {
            throw new ApiException(HttpStatus.CONFLICT.value(), MessageCode.BAD_REQUEST, "Question already answered");
        }

        Answer answer = answerRepository.findById(request.getAnswerId()).orElseThrow();
        if (!request.getQuestionId().equals(answer.getQuestionId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), MessageCode.BAD_REQUEST, "Answer does not belong to the submitted question");
        }

        boolean isCorrect = answer.getIsCorrect();

        // Lấy thông tin phòng để tính điểm
        Room room = roomRepository.findById(roomId).orElseThrow();
        long serverTimeTaken = Math.max(0, Math.min(now - startedAt, room.getCountdownTime() * 1000L));

        // Tính điểm dựa trên tốc độ và độ chính xác
        int baseScore = scoreCalculator.calculateScore(isCorrect, serverTimeTaken, room.getCountdownTime());

        int finalScore = baseScore;
        int currentStreak = 0;
        double streakMultiplier = 1.0;

        if (isCorrect) {
            // Tính streak hiện tại của người chơi
            currentStreak = calculatePlayerStreak(userId, roomId);

            // Áp dụng streak multiplier
            finalScore = scoreCalculator.applyStreakMultiplier(currentStreak, baseScore);

            // Tính multiplier để hiển thị cho frontend
            if (currentStreak >= 10) {
                streakMultiplier = 2.0;
            } else if (currentStreak >= 5) {
                streakMultiplier = 1.5;
            } else if (currentStreak >= 3) {
                streakMultiplier = 1.2;
            }
        }

        // Lưu user answer với điểm đã áp dụng streak bonus
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setUserId(userId);
        userAnswer.setQuestionId(request.getQuestionId());
        userAnswer.setRoomId(roomId);
        userAnswer.setAnswerId(request.getAnswerId());
        userAnswer.setIsCorrect(isCorrect);
        userAnswer.setScore(finalScore);
        userAnswer.setTimeTaken((int) Math.min(serverTimeTaken, Integer.MAX_VALUE));
        try {
            userAnswerRepository.saveAndFlush(userAnswer);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT.value(), MessageCode.BAD_REQUEST, "Question already answered");
        }

        QuestionResultResponse response = new QuestionResultResponse();
        response.setIsCorrect(isCorrect);
        response.setScore(finalScore);
        response.setTimeTaken(serverTimeTaken);
        response.setServerTimeTaken(serverTimeTaken);
        response.setAcceptedAt(now);
        response.setStreak(currentStreak);
        response.setStreakMultiplier(streakMultiplier);

        return response;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Long.parseLong(stringValue);
        }
        return null;
    }

    private int calculatePlayerStreak(Long userId, Long roomId) {
        // Lấy tất cả câu trả lời của user trong room này, sắp xếp theo thời gian
        List<UserAnswer> userAnswers = userAnswerRepository.findByRoomIdAndUserId(roomId, userId);

        if (userAnswers.isEmpty()) {
            return 0;
        }

        // Sắp xếp theo thời gian tạo (câu mới nhất ở cuối)
        userAnswers.sort(Comparator.comparing(UserAnswer::getCreatedAt));

        // Đếm streak từ cuối danh sách về đầu
        int streak = 0;
        for (int i = userAnswers.size() - 1; i >= 0; i--) {
            if (userAnswers.get(i).getIsCorrect()) {
                streak++;
            } else {
                break; // Gặp câu sai thì dừng
            }
        }

        return streak;
    }

    @Override
    @Transactional
    public GameOverResponse endGame(Long roomId) {
        synchronized (roomLock(roomId)) {
            return endGameLocked(roomId);
        }
    }

    private GameOverResponse endGameLocked(Long roomId) {
        String gameId = "game:" + roomId;
        Map<String, Object> sessionData = redisService.getGameSession(gameId);

        if (sessionData == null) {
            throw new RuntimeException("Game session not found");
        }

        Long gameSessionId = ((Number) sessionData.get("gameSessionId")).longValue();

        // Cập nhật game session
        GameSession gameSession = gameSessionRepository.findById(gameSessionId).orElseThrow();
        gameSession.setGameStatus(GameStatus.FINISHED.name());
        gameSession.setEndTime(LocalDateTime.now());
        gameSessionRepository.save(gameSession);

        // Tính ranking
        List<UserAnswer> allAnswers = userAnswerRepository.findByRoomId(roomId);
        Map<Long, Integer> userScores = new HashMap<>();
        Map<Long, Long> userTimes = new HashMap<>();
        Map<Long, String> userNames = new HashMap<>();
        Map<Long, String> userAvatars = new HashMap<>();
        Map<Long, Integer> userCorrectAnswers = new HashMap<>();
        Map<Long, Integer> userTotalAnswers = new HashMap<>();

        List<RoomPlayers> players = roomPlayerRepository.findByRoomId(roomId);
        Set<Long> userIds = players.stream().map(RoomPlayers::getUserId).collect(Collectors.toSet());
        Map<Long, User> userById = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        for (RoomPlayers player : players) {
            User user = userById.get(player.getUserId());
            if (user != null) {
                userNames.put(player.getUserId(), user.getUsername());
                userAvatars.put(player.getUserId(), user.getAvatarURL());
            } else {
                userNames.put(player.getUserId(), "User" + player.getUserId());
                userAvatars.put(player.getUserId(), null);
            }
            userScores.put(player.getUserId(), 0);
            userTimes.put(player.getUserId(), 0L);
            userCorrectAnswers.put(player.getUserId(), 0);
            userTotalAnswers.put(player.getUserId(), 0);
        }

        // Tính tổng điểm, thời gian và độ chính xác
        for (UserAnswer answer : allAnswers) {
            Long userId = answer.getUserId();
            userScores.put(userId, userScores.getOrDefault(userId, 0) + answer.getScore());
            userTimes.put(userId, userTimes.getOrDefault(userId, 0L) + answer.getTimeTaken());
            userTotalAnswers.put(userId, userTotalAnswers.getOrDefault(userId, 0) + 1);
            if (answer.getIsCorrect()) {
                userCorrectAnswers.put(userId, userCorrectAnswers.getOrDefault(userId, 0) + 1);
            }
        }

        // Tạo ranking với đầy đủ thông tin
        List<PlayerRanking> rankings = userScores.entrySet().stream()
                .sorted((e1, e2) -> {
                    int scoreCompare = e2.getValue().compareTo(e1.getValue());
                    if (scoreCompare == 0) {
                        return userTimes.get(e1.getKey()).compareTo(userTimes.get(e2.getKey()));
                    }
                    return scoreCompare;
                })
                .map(entry -> {
                    Long userId = entry.getKey();
                    PlayerRanking ranking = new PlayerRanking();
                    ranking.setUserId(userId);
                    ranking.setUserName(userNames.get(userId));
                    ranking.setAvatar(userAvatars.get(userId));
                    ranking.setTotalScore(entry.getValue());
                    ranking.setTotalTime(userTimes.get(userId));
                    ranking.setCorrectAnswers(userCorrectAnswers.getOrDefault(userId, 0));
                    ranking.setTotalAnswers(userTotalAnswers.getOrDefault(userId, 0));
                    return ranking;
                })
                .collect(Collectors.toList());

        // Set rank
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRank(i + 1);
        }

        // Tạo user scores với đầy đủ thông tin
        List<PlayerScore> playerScores = allAnswers.stream()
                .collect(Collectors.groupingBy(UserAnswer::getUserId))
                .entrySet().stream()
                .map(entry -> {
                    Long userId = entry.getKey();
                    PlayerScore score = new PlayerScore();
                    score.setUserId(userId);
                    score.setUserName(userNames.get(userId));
                    score.setAvatar(userAvatars.get(userId));
                    score.setScore(userScores.get(userId));
                    score.setTimeTaken(userTimes.get(userId));
                    return score;
                })
                .collect(Collectors.toList());

        // Lưu game history và cập nhật rank (FIXED: Kiểm tra duplicate)
        int totalQuestions = ((Number) sessionData.get("totalQuestions")).intValue();
        for (PlayerRanking ranking : rankings) {
            // Kiểm tra xem đã có history chưa
            boolean historyExists = gameHistoryRepository
                .existsByGameSessionIdAndUserId(gameSessionId, ranking.getUserId());
            
            if (!historyExists) {
                GameHistory history = new GameHistory();
                history.setGameSessionId(gameSessionId);
                history.setUserId(ranking.getUserId());
                history.setScore(ranking.getTotalScore().intValue());

                int correctCount = userCorrectAnswers.getOrDefault(ranking.getUserId(), 0);
                history.setCorrectAnswers(correctCount);
                history.setTotalQuestions(totalQuestions);
                gameHistoryRepository.save(history);

                gameAsyncPostProcessor.updateRankAndProfileAfterGame(
                        ranking.getUserId(),
                        ranking.getTotalScore().intValue(),
                        ranking.getTotalTime(),
                        roomId
                );
                
                log.info("Saved game history for user {} in session {}", ranking.getUserId(), gameSessionId);
            } else {
                log.info("Game history already exists for user {} in session {}", ranking.getUserId(), gameSessionId);
            }
        }

        // Cập nhật Redis
        redisService.updateGameStatus(gameId, GameStatus.FINISHED);

        // Cập nhật trạng thái phòng về FINISHED
        Room room = roomRepository.findById(roomId).orElseThrow();
        room.setStatus(RoomStatus.FINISHED.name());
        roomRepository.save(room);

        GameOverResponse response = new GameOverResponse();
        response.setRanking(rankings);
        response.setUserScores(playerScores);

        log.info("Ended game session {} for room {}", gameSessionId, roomId);

        return response;
    }

    /*** Xử lý event khi hết thời gian trả lời ***/
    @EventListener
    public void handleTimerFinished(GameTimerEvent event) {
        try {
            Long roomId = event.getRoomId();
            log.info("Timer finished for room {}", roomId);

            synchronized (roomLock(roomId)) {
                String gameId = "game:" + roomId;
                Map<String, Object> sessionData = redisService.getGameSession(gameId);
                if (sessionData == null || !GameStatus.IN_PROGRESS.name().equals(sessionData.get("status"))) {
                    return;
                }

                int currentIndex = ((Number) sessionData.get("currentQuestionIndex")).intValue();
                int totalQuestions = ((Number) sessionData.get("totalQuestions")).intValue();
                Room room = roomRepository.findById(roomId).orElseThrow();

                if (currentIndex < totalQuestions) {
                    NextQuestionResponse nextQuestion = getNextQuestion(roomId);
                    if (nextQuestion != null) {
                        gameSocketFacade.notifyNextQuestion(room.getRoomCode(), nextQuestion);
                        gameTimerService.startGameTimer(roomId, room.getRoomCode(), nextQuestion.getTimeLimit());
                    }
                    return;
                }

                GameOverResponse gameResult = endGameLocked(roomId);
                gameSocketFacade.notifyGameFinished(room.getRoomCode(), gameResult);
            }
        } catch (Exception e) {
            log.error("Error handling timer event: {}", e.getMessage(), e);
        }
    }

    private Object roomLock(Long roomId) {
        return roomLocks.computeIfAbsent(roomId, ignored -> new Object());
    }

    @Override
    public Long resolveAnswerId(Long questionId, Integer selectedOptionIndex, String selectedAnswer, String answerText) {
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        if (answers == null || answers.isEmpty()) {
            throw new RuntimeException("No answers found for question " + questionId);
        }
        // Prefer match by text first
        if (selectedAnswer != null) {
            Optional<Answer> match = answers.stream()
                    .filter(a -> selectedAnswer.equalsIgnoreCase(a.getAnswerText()))
                    .findFirst();
            if (match.isPresent()) return match.get().getId();
        }
        if (answerText != null) {
            Optional<Answer> match = answers.stream()
                    .filter(a -> answerText.equalsIgnoreCase(a.getAnswerText()))
                    .findFirst();
            if (match.isPresent()) return match.get().getId();
        }
        // Fallback by index if provided (0-based)
        if (selectedOptionIndex != null) {
            // Ensure a deterministic order
            answers.sort(Comparator.comparing(Answer::getId));
            if (selectedOptionIndex >= 0 && selectedOptionIndex < answers.size()) {
                return answers.get(selectedOptionIndex).getId();
            }
        }
        throw new RuntimeException("Cannot resolve answerId from provided selection for question " + questionId);
    }

    @Override
    public NextQuestionResponse getNextQuestionForPlayer(Long roomId, Long userId) {
        try {
            String gameId = "game:" + roomId;
            Map<String, Object> sessionData = redisService.getGameSession(gameId);

            if (sessionData == null || !GameStatus.IN_PROGRESS.name().equals(sessionData.get("status"))) {
                log.warn("Game not active for room {}", roomId);
                return null;
            }

            int totalQuestions = ((Number) sessionData.get("totalQuestions")).intValue();
            Long gameSessionId = ((Number) sessionData.get("gameSessionId")).longValue();

            // Đếm số câu hỏi player này đã trả lời
            List<UserAnswer> playerAnswers = userAnswerRepository.findByRoomIdAndUserId(roomId, userId);
            int answeredCount = playerAnswers.size();

            log.info("Player {} has answered {}/{} questions in room {}",
                    userId, answeredCount, totalQuestions, roomId);

            // Nếu đã trả lời hết -> return null
            if (answeredCount >= totalQuestions) {
                log.info("Player {} has completed all questions in room {}", userId, roomId);
                return null;
            }

            // Lấy câu hỏi tiếp theo (index = số câu đã trả lời)
            Room room = roomRepository.findById(roomId).orElseThrow();
            List<GameQuestion> gameQuestions = gameQuestionRepository
                    .findByGameSessionIdOrderByQuestionOrder(gameSessionId);

            if (answeredCount >= gameQuestions.size()) {
                return null;
            }

            GameQuestion nextGameQuestion = gameQuestions.get(answeredCount);
            Question question = questionRepository.findById(nextGameQuestion.getQuestionId()).orElseThrow();

            // Lấy đáp án và shuffle để tráo vị trí
            List<Answer> answers = answerRepository.findByQuestionId(question.getId());
            Collections.shuffle(answers); // Tráo đáp án để đáp án đúng không luôn ở vị trí A
            List<AnswerOption> answerOptions = answers.stream()
                    .map(a -> new AnswerOption(a.getId(), a.getAnswerText()))
                    .collect(Collectors.toList());

            NextQuestionResponse response = new NextQuestionResponse();
            response.setQuestionId(question.getId());
            response.setQuestionText(question.getQuestionText());
            response.setAnswers(answerOptions);
            response.setTimeLimit(room.getCountdownTime());
            response.setQuestionNumber(answeredCount + 1);
            response.setTotalQuestions(totalQuestions);

            log.info("Returning question {} for player {} in room {}",
                    answeredCount + 1, userId, roomId);

            return response;
        } catch (Exception e) {
            log.error("Error getting next question for player: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean haveAllPlayersCompleted(Long roomId) {
        try {
            String gameId = "game:" + roomId;
            Map<String, Object> sessionData = redisService.getGameSession(gameId);

            if (sessionData == null) {
                log.warn("No game session for room {}", roomId);
                return false;
            }

            int totalQuestions = ((Number) sessionData.get("totalQuestions")).intValue();

            // Lấy danh sách players trong room
            List<RoomPlayers> roomPlayers = roomPlayerRepository.findByRoomIdOrderByJoinOrder(roomId);
            int totalPlayers = roomPlayers.size();

            if (totalPlayers == 0) {
                log.warn("No players in room {}", roomId);
                return false;
            }

            Map<Long, Long> answerCountByUser = userAnswerRepository.countAnswersByUserInRoom(roomId).stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0],
                            row -> (Long) row[1]
                    ));

            int completedPlayers = (int) roomPlayers.stream()
                    .map(RoomPlayers::getUserId)
                    .filter(userId -> answerCountByUser.getOrDefault(userId, 0L) >= totalQuestions)
                    .count();

            return completedPlayers >= totalPlayers;
        } catch (Exception e) {
            log.error("Error checking if all players completed: {}", e.getMessage(), e);
            return false;
        }
    }
}
