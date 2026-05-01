package org.example.quizizz.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quizizz.controller.socketio.listener.RoomListEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lắng nghe các sự kiện liên quan đến phòng và broadcast tới client subscribe room-list.
 *
 * Dùng @TransactionalEventListener(AFTER_COMMIT) thay vì @EventListener để đảm bảo:
 *  - Room/update/delete đã commit thực sự vào DB rồi mới broadcast.
 *  - Tránh trường hợp broadcast room mới rồi transaction rollback → client thấy "phòng ma".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomEventListener {

    private final RoomListEventHandler roomListEventHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleRoomEvent(RoomEvent event) {
        try {
            switch (event.getEventType()) {
                case ROOM_CREATED:
                    roomListEventHandler.notifyRoomCreated(event.getData());
                    break;
                case ROOM_UPDATED:
                    roomListEventHandler.notifyRoomUpdated(event.getData());
                    break;
                case ROOM_DELETED:
                    roomListEventHandler.notifyRoomDeleted((Long) event.getData());
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to broadcast room event {}: {}", event.getEventType(), e.getMessage(), e);
        }
    }
}