package org.example.quizizz.service.helper;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quizizz.controller.socketio.broadcast.GameSocketFacade;
import org.example.quizizz.service.Interface.IRedisService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class GameTimerService {

    private final GameSocketFacade gameSocketFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final IRedisService redisService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<Long, GameTimerState> activeTimers = new ConcurrentHashMap<>();

    public void startGameTimer(Long roomId, int timeLimit) {
        startGameTimer(roomId, roomId != null ? roomId.toString() : null, timeLimit);
    }

    public void startGameTimer(Long roomId, String roomCode, int timeLimit) {
        if (roomId == null || roomCode == null || roomCode.isBlank()) {
            return;
        }

        int safeTimeLimit = Math.max(0, timeLimit);
        stopGameTimer(roomId);

        long startedAt = System.currentTimeMillis();
        long expiresAt = startedAt + safeTimeLimit * 1000L;

        GameTimerState timer = new GameTimerState(roomId, roomCode, safeTimeLimit, startedAt, expiresAt);
        activeTimers.put(roomId, timer);

        broadcastTimerUpdate(timer);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                timer.tick();
                broadcastTimerUpdate(timer);

                if (timer.isFinished()) {
                    handleTimerFinished(timer);
                }
            } catch (Exception e) {
                log.error("Error in game timer for room {}: {}", roomId, e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);

        timer.setFuture(future);
    }

    public void stopGameTimer(Long roomId) {
        GameTimerState timer = activeTimers.remove(roomId);
        if (timer != null) {
            timer.cancel();
        }
    }

    public Integer getRemainingTime(Long roomId) {
        GameTimerState timer = activeTimers.get(roomId);
        if (timer != null) {
            return timer.getRemainingTime();
        }

        Map<String, Object> sessionData = redisService.getGameSession("game:" + roomId);
        Long expiresAt = asLong(sessionData.get("currentQuestionExpiresAt"));
        if (expiresAt == null) {
            return null;
        }
        long remainingMillis = Math.max(0L, expiresAt - System.currentTimeMillis());
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            return Long.parseLong(string);
        }
        return null;
    }

    private void broadcastTimerUpdate(GameTimerState timer) {
        try {
            gameSocketFacade.notifyCountdownTick(
                timer.getRoomCode(),
                timer.getRoomId(),
                timer.getRemainingTime(),
                timer.getStartedAt(),
                timer.getExpiresAt()
            );
        } catch (Exception e) {
            log.error("Error broadcasting timer update for room {}: {}", timer.getRoomId(), e.getMessage());
        }
    }

    private void handleTimerFinished(GameTimerState timer) {
        stopGameTimer(timer.getRoomId());

        try {
            gameSocketFacade.notifyTimeUp(timer.getRoomCode(), timer.getRoomId());
            eventPublisher.publishEvent(new GameTimerEvent(this, timer.getRoomId()));
        } catch (Exception e) {
            log.error("Error handling timer finished for room {}: {}", timer.getRoomId(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        activeTimers.values().forEach(GameTimerState::cancel);
        scheduler.shutdownNow();
    }

    private static class GameTimerState {
        private final Long roomId;
        private final String roomCode;
        private final AtomicInteger remainingTime;
        private final long startedAt;
        private final long expiresAt;
        private volatile boolean cancelled = false;
        private volatile ScheduledFuture<?> future;

        private GameTimerState(Long roomId, String roomCode, int totalTime, long startedAt, long expiresAt) {
            this.roomId = roomId;
            this.roomCode = roomCode;
            this.remainingTime = new AtomicInteger(totalTime);
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
        }

        private void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        private void tick() {
            if (!cancelled) {
                remainingTime.updateAndGet(current -> current > 0 ? current - 1 : 0);
            }
        }

        private boolean isFinished() {
            return !cancelled && remainingTime.get() <= 0;
        }

        private int getRemainingTime() {
            return remainingTime.get();
        }

        private Long getRoomId() {
            return roomId;
        }

        private String getRoomCode() {
            return roomCode;
        }

        private long getStartedAt() {
            return startedAt;
        }

        private long getExpiresAt() {
            return expiresAt;
        }

        private void cancel() {
            cancelled = true;
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}
