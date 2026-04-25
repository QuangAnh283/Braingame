package org.example.quizizz.service.Implement;

import org.example.quizizz.common.constants.MessageCode;
import org.example.quizizz.common.constants.RoomMode;
import org.example.quizizz.common.constants.RoomStatus;
import org.example.quizizz.common.exception.ApiException;
import org.example.quizizz.common.event.RoomEvent;
import org.example.quizizz.controller.socketio.broadcast.RoomSocketFacade;
import org.example.quizizz.mapper.RoomMapper;
import org.example.quizizz.mapper.RoomPlayerMapper;
import org.example.quizizz.model.dto.room.*;
import org.example.quizizz.model.entity.Room;
import org.example.quizizz.model.entity.RoomPlayers;
import org.example.quizizz.model.entity.User;
import org.example.quizizz.repository.*;
import org.example.quizizz.service.Interface.IRoomService;
import org.example.quizizz.util.PageableUtil;
import org.example.quizizz.util.RoomCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImplement implements IRoomService {

    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final ExamRepository examRepository;
    private final RoomSocketFacade roomSocketFacade;
    private final RoomMapper roomMapper;
    private final RoomPlayerMapper roomPlayerMapper;
    private final RoomCodeGenerator roomCodeGenerator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tạo phòng
     * @param request thông tin tạo phòng
     * @param userId  ID người tạo phòng
     * @return
     */
    @Override
    public RoomResponse createRoom(CreateRoomRequest request, Long userId) {
        validateRoomModeAndMaxPlayers(request.getRoomMode(), request.getMaxPlayers());

        Room room = roomMapper.toEntity(request);
        room.setOwnerId(userId);
        room.setStatus(RoomStatus.WAITING.name());
        room.setRoomCode(generateUniqueRoomCode());

        if (request.getMaxPlayers() == null) {
            room.setMaxPlayers(request.getRoomMode() == RoomMode.ONE_VS_ONE ? 2 : 10);
        }

        Room savedRoom = roomRepository.save(room);

        RoomPlayers hostPlayer = new RoomPlayers();
        hostPlayer.setRoomId(savedRoom.getId());
        hostPlayer.setUserId(userId);
        hostPlayer.setIsHost(true);
        hostPlayer.setJoinOrder(1);
        hostPlayer.setStatus("ACTIVE");
        roomPlayerRepository.save(hostPlayer);

        log.info("Room {} created successfully by user {}", savedRoom.getId(), userId);

        RoomResponse roomResponse = mapToRoomResponse(savedRoom);

        eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_CREATED, roomResponse));

        return roomResponse;
    }

    @Override
    public RoomResponse joinRoom(JoinRoomRequest request, Long userId) {
        Room room = roomRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        if (room.getStatus().equals(RoomStatus.PLAYING.name()) || room.getStatus().equals(RoomStatus.FINISHED.name())) {
            throw new ApiException(MessageCode.ROOM_ALREADY_STARTED);
        }

        if (room.getStatus().equals(RoomStatus.FULL.name())) {
            throw new ApiException(MessageCode.ROOM_FULL);
        }

        Integer currentPlayers = roomPlayerRepository.countPlayersInRoom(room.getId());
        if (currentPlayers >= room.getMaxPlayers()) {
            throw new ApiException(MessageCode.ROOM_FULL);
        }

        if (roomPlayerRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            // User already in room - return success with room data
            log.info("User {} already in room {}, returning room data", userId, room.getId());
            return mapToRoomResponse(room);
        }

        // Điều này tránh lỗi permission denied khi join phòng private lần đầu
        if (roomPlayerRepository.isUserKicked(room.getId(), userId)) {
            log.warn("User {} is kicked from room {}", userId, room.getId());
            throw new ApiException(MessageCode.ROOM_PERMISSION_DENIED);
        }

        Integer nextJoinOrder = roomPlayerRepository.getMaxJoinOrderInRoom(room.getId()) + 1;

        RoomPlayers player = new RoomPlayers();
        player.setRoomId(room.getId());
        player.setUserId(userId);
        player.setIsHost(false);
        player.setJoinOrder(nextJoinOrder);
        player.setStatus("ACTIVE");
        roomPlayerRepository.save(player);

        // Cập nhật trạng thái phòng nếu đủ người
        updateRoomStatus(room);

        RoomResponse roomResponse = mapToRoomResponse(room);
        eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_UPDATED, roomResponse));

        log.info("User {} successfully joined room {}", userId, room.getId());
        return roomResponse;
    }

    /**
     * Thoát phòng
     * @param roomId ID phòng
     * @param userId ID người rời phòng
     */
    @Override
    public void leaveRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        if (!roomPlayerRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ApiException(MessageCode.ROOM_NOT_JOINED);
        }

        if (room.getOwnerId().equals(userId)) {
            handleHostLeaving(room);
        } else {
            roomPlayerRepository.deleteByRoomIdAndUserId(roomId, userId);
            updateRoomStatus(room);
        }

        Room roomAfterLeave = roomRepository.findById(roomId).orElse(null);
        if (roomAfterLeave != null) {
            RoomResponse response = mapToRoomResponse(roomAfterLeave);
            eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_UPDATED, response));
        } else {
            eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_DELETED, roomId));
        }
    }

    /**
     * Lấy danh sách người chơi trong phòng
     * @param roomId ID phòng
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomPlayerResponse> getRoomPlayers(Long roomId) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        List<RoomPlayers> players = roomPlayerRepository.findByRoomIdOrderByJoinOrder(roomId);
        Set<Long> userIds = players.stream()
                .map(RoomPlayers::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userById = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return players.stream()
                .map(player -> {
                    User user = userById.get(player.getUserId());
                    return user != null ? roomPlayerMapper.toResponse(player, user)
                            : roomPlayerMapper.toResponse(player);
                })
                .collect(Collectors.toList());
    }

    /**
     * Kick player
     * @param roomId  ID phòng
     * @param request thông tin kick
     * @param hostId  ID host
     */
    @Override
    public void kickPlayer(Long roomId, KickPlayerRequest request, Long hostId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        if (!isRoomHost(roomId, hostId)) {
            throw new ApiException(MessageCode.ROOM_PERMISSION_DENIED);
        }

        if (hostId.equals(request.getPlayerId())) {
            throw new ApiException(MessageCode.BAD_REQUEST);
        }

        if (!roomPlayerRepository.existsByRoomIdAndUserId(roomId, request.getPlayerId())) {
            throw new ApiException(MessageCode.PLAYER_NOT_IN_GAME);
        }

        RoomPlayers player = roomPlayerRepository.findByRoomIdAndUserId(roomId, request.getPlayerId()).get();
        player.setStatus("KICKED");
        roomPlayerRepository.save(player);

        eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_UPDATED, mapToRoomResponse(room)));
    }

    @Override
    public InvitationResponse invitePlayer(InvitePlayerRequest request, Long inviterId) {
        // TODO: Implement invitation logic later using Redis
        return null;
    }

    @Override
    public RoomResponse respondToInvitation(Long invitationId, boolean accept, Long userId) {
        // TODO: Implement invitation logic later
        throw new ApiException(MessageCode.NOT_IMPLEMENTED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> getUserInvitations(Long userId) {
        // TODO: Implement invitation logic later
        return List.of();
    }

    private void handleHostLeaving(Room room) {
        List<RoomPlayers> players = roomPlayerRepository.findByRoomIdOrderByJoinOrder(room.getId());
        roomPlayerRepository.deleteByRoomIdAndUserId(room.getId(), room.getOwnerId());

        if (players.size() <= 1) {
            handleEmptyRoom(room);
        } else {
            Long previousHostId = room.getOwnerId();

            RoomPlayers nextHost = players.stream()
                    .filter(p -> !p.getUserId().equals(room.getOwnerId()) && "ACTIVE".equals(p.getStatus()))
                    .min((p1, p2) -> p1.getJoinOrder().compareTo(p2.getJoinOrder()))
                    .orElseThrow(() -> new ApiException(MessageCode.PLAYER_NOT_IN_GAME));

            room.setOwnerId(nextHost.getUserId());
            roomRepository.save(room);

            nextHost.setIsHost(true);
            roomPlayerRepository.save(nextHost);

            roomSocketFacade.notifyHostChanged(
                    room.getRoomCode(),
                    room.getId(),
                    nextHost.getUserId(),
                    previousHostId,
                    "previous_host_left"
            );
        }
    }

    /**
     * Xử lý khi phòng trống
     * Case 1: Chưa có game history -> Xóa phòng
     * Case 2: Đã có game history -> Archive phòng
     */
    private void handleEmptyRoom(Room room) {
        if (!room.getHasGameHistory()) {
            roomPlayerRepository.deleteByRoomId(room.getId());
            roomRepository.delete(room);
            eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_DELETED, room.getId()));
        } else {
            room.setStatus(RoomStatus.ARCHIVED.name());
            roomRepository.save(room);
            roomPlayerRepository.deleteByRoomId(room.getId());
            eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_DELETED, room.getId()));
        }
    }

    @Override
    public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request, Long userId) {
        return null;
    }

    @Override
    public void deleteRoom(Long roomId, Long userId) {
        // Kiểm tra phòng tồn tại
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        // Kiểm tra quyền xóa phòng (chỉ owner mới được xóa)
        if (!room.getOwnerId().equals(userId)) {
            throw new ApiException(MessageCode.UNAUTHORIZED);
        }

        // Xóa tất cả players trong phòng trước (3NF - manual delete)
        roomPlayerRepository.deleteByRoomId(roomId);

        // Xóa phòng
        roomRepository.deleteById(roomId);

        log.info("Room {} deleted by user {} - room players manually deleted", roomId, userId);

        eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_DELETED, roomId));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));
        return mapToRoomResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomByCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));
        return mapToRoomResponse(room);
    }

    @Override
    public RoomResponse transferHost(Long roomId, Long newHostId, Long currentHostId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        if (!isRoomHost(roomId, currentHostId)) {
            throw new ApiException(MessageCode.ROOM_PERMISSION_DENIED);
        }

        if (!isUserInRoom(roomId, newHostId)) {
            throw new ApiException(MessageCode.PLAYER_NOT_IN_GAME);
        }

        room.setOwnerId(newHostId);
        Room updatedRoom = roomRepository.save(room);

        RoomPlayers oldHost = roomPlayerRepository.findByRoomIdAndUserId(roomId, currentHostId).get();
        RoomPlayers newHost = roomPlayerRepository.findByRoomIdAndUserId(roomId, newHostId).get();

        oldHost.setIsHost(false);
        newHost.setIsHost(true);

        roomPlayerRepository.save(oldHost);
        roomPlayerRepository.save(newHost);

        RoomResponse response = mapToRoomResponse(updatedRoom);

        // Broadcast host-changed và và cập nhật danh sách người chơi
        roomSocketFacade.notifyHostChanged(room.getRoomCode(), roomId, newHostId, currentHostId, "manual_transfer");
        List<RoomPlayerResponse> players = getRoomPlayers(roomId);
        roomSocketFacade.notifyRoomPlayers(room.getRoomCode(), roomId, players);

        return response;
    }

    @Override
    public void startGame(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        if (!isRoomHost(roomId, userId)) {
            throw new ApiException(MessageCode.ROOM_PERMISSION_DENIED);
        }

        if (!room.getStatus().equals(RoomStatus.WAITING.name())) {
            throw new ApiException(MessageCode.GAME_ALREADY_STARTED);
        }

        room.setStatus(RoomStatus.PLAYING.name());
        room.setHasGameHistory(true);
        roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRoomHost(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));
        return room.getOwnerId().equals(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserInRoom(Long roomId, Long userId) {
        return roomPlayerRepository.existsByRoomIdAndUserId(roomId, userId);
    }

    private void validateRoomModeAndMaxPlayers(RoomMode roomMode, Integer maxPlayers) {
        if (roomMode == RoomMode.ONE_VS_ONE && maxPlayers != null && maxPlayers != 2) {
            throw new ApiException(MessageCode.ROOM_INVALID_MAX_PLAYERS);
        }
        if (roomMode == RoomMode.BATTLE_ROYAL && maxPlayers != null && (maxPlayers < 3 || maxPlayers > 50)) {
            throw new ApiException(MessageCode.ROOM_INVALID_MAX_PLAYERS);
        }
    }

    @Override
    public PagedRoomResponse getAllRoomsSimple(int page, int size, String search) {
        Pageable pageable = PageableUtil.createPageable(page, size, "createdAt,desc");
        Page<Room> roomPage;

        // Chỉ lấy phòng WAITING, không filter phức tạp
        if (search != null && !search.trim().isEmpty()) {
            roomPage = roomRepository.findAllRoomsByStatusAndSearch(RoomStatus.WAITING.name(), search.trim(), pageable);
        } else {
            roomPage = roomRepository.findAllRoomsByStatusWithPagination(RoomStatus.WAITING.name(), pageable);
        }

        return mapToPagedResponse(roomPage);
    }

    @Override
    public RoomResponse joinRoomById(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ApiException(MessageCode.ROOM_NOT_FOUND));

        if (room.getIsPrivate()) {
            throw new ApiException(MessageCode.ROOM_PERMISSION_DENIED);
        }

        if (room.getStatus().equals(RoomStatus.PLAYING.name()) || room.getStatus().equals(RoomStatus.FINISHED.name())) {
            throw new ApiException(MessageCode.ROOM_ALREADY_STARTED);
        }

        if (room.getStatus().equals(RoomStatus.FULL.name())) {
            throw new ApiException(MessageCode.ROOM_FULL);
        }

        Integer currentPlayers = roomPlayerRepository.countPlayersInRoom(room.getId());
        if (currentPlayers >= room.getMaxPlayers()) {
            throw new ApiException(MessageCode.ROOM_FULL);
        }

        if (roomPlayerRepository.existsByRoomIdAndUserId(room.getId(), userId)) {
            log.info("User {} already in room {}, returning room data", userId, room.getId());
            return mapToRoomResponse(room);
        }

        Integer nextJoinOrder = roomPlayerRepository.getMaxJoinOrderInRoom(room.getId()) + 1;

        RoomPlayers player = new RoomPlayers();
        player.setRoomId(room.getId());
        player.setUserId(userId);
        player.setIsHost(false);
        player.setJoinOrder(nextJoinOrder);
        player.setStatus("ACTIVE");
        roomPlayerRepository.save(player);

        updateRoomStatus(room);

        RoomResponse roomResponse = mapToRoomResponse(room);
        eventPublisher.publishEvent(new RoomEvent(this, RoomEvent.Type.ROOM_UPDATED, roomResponse));

        return roomResponse;
    }

    /**
     * Cập nhật trạng thái phòng dựa trên số lượng người chơi
     */
    private void updateRoomStatus(Room room) {
        if (room.getStatus().equals(RoomStatus.PLAYING.name()) ||
                room.getStatus().equals(RoomStatus.FINISHED.name()) ||
                room.getStatus().equals(RoomStatus.ARCHIVED.name())) {
            return; // Không thay đổi trạng thái nếu đang chơi hoặc đã kết thúc
        }

        Integer currentPlayers = roomPlayerRepository.countPlayersInRoom(room.getId());

        if (currentPlayers >= room.getMaxPlayers()) {
            room.setStatus(RoomStatus.FULL.name());
        } else {
            room.setStatus(RoomStatus.WAITING.name());
        }

        roomRepository.save(room);
    }

    private PagedRoomResponse mapToPagedResponse(Page<Room> roomPage) {
        PagedRoomResponse response = new PagedRoomResponse();
        response.setRooms(mapToRoomResponses(roomPage.getContent()));
        response.setCurrentPage(roomPage.getNumber());
        response.setTotalPages(roomPage.getTotalPages());
        response.setTotalElements(roomPage.getTotalElements());
        response.setPageSize(roomPage.getSize());
        response.setHasNext(roomPage.hasNext());
        response.setHasPrevious(roomPage.hasPrevious());
        return response;
    }

    private List<RoomResponse> mapToRoomResponses(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());

        Map<Long, Integer> playerCountByRoomId = roomPlayerRepository.countPlayersInRooms(roomIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        Set<Long> topicIds = rooms.stream()
                .map(Room::getTopicId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> topicNameById = topicIds.isEmpty()
                ? Collections.emptyMap()
                : topicRepository.findAllById(topicIds).stream()
                .collect(Collectors.toMap(topic -> topic.getId(), topic -> topic.getName()));

        Set<Long> examIds = rooms.stream()
                .map(Room::getExamId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> examTitleById = examIds.isEmpty()
                ? Collections.emptyMap()
                : examRepository.findAllById(examIds).stream()
                .collect(Collectors.toMap(exam -> exam.getId(), exam -> exam.getTitle()));

        Set<Long> ownerIds = rooms.stream()
                .map(Room::getOwnerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> ownerUsernameById = ownerIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return rooms.stream()
                .map(room -> {
                    RoomResponse response = roomMapper.toResponse(room);
                    response.setCurrentPlayers(playerCountByRoomId.getOrDefault(room.getId(), 0));

                    if (room.getTopicId() != null) {
                        response.setTopicName(topicNameById.get(room.getTopicId()));
                    }
                    if (room.getExamId() != null) {
                        response.setExamTitle(examTitleById.get(room.getExamId()));
                    }
                    if (room.getOwnerId() != null) {
                        response.setOwnerUsername(ownerUsernameById.get(room.getOwnerId()));
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Map Room entity to RoomResponse với currentPlayers và topicName
     */
    private RoomResponse mapToRoomResponse(Room room) {
        List<RoomResponse> responses = mapToRoomResponses(List.of(room));
        return responses.isEmpty() ? roomMapper.toResponse(room) : responses.get(0);
    }

    private String generateUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = roomCodeGenerator.generateRoomCode();
        } while (roomRepository.existsByRoomCode(roomCode));
        return roomCode;
    }

    @Override
    public RoomRepository getRoomRepository() {
        return roomRepository;
    }
}
