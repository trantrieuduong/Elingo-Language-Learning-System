package com.elingo.battle.service.impl;

import com.elingo.battle.dto.response.RoomCreatedResponse;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.service.BattleMatchService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import com.elingo.battle.state.BattleRoomRegistry;
import com.elingo.battle.state.LiveRoom;
import com.elingo.battle.util.BattleRoomCode;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j(topic = "BATTLE-ROOM-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleRoomServiceImpl implements BattleRoomService {

    private final BattleRoomRegistry registry;
    private final BattleMatchService matchService;
    private final BattleMatchPersistenceService persistenceService;
    private final BattleQueueService queueService;
    private final BattleEventPublisher eventPublisher;

    @Override
    public synchronized void createRoom(Long userId, BattleMode mode) {
        assertUserFree(userId);
        // Nếu user đang host phòng cũ thì gỡ để tạo phòng mới
        registry.removeByHost(userId);

        String code = null;
        for (int i = 0; i < 10; i++) {
            String candidate = BattleRoomCode.generate();
            if (!registry.codeExists(candidate) && !persistenceService.roomCodeExists(candidate)) {
                code = candidate;
                break;
            }
        }
        if (code == null) {
            log.error("Failed to generate unique room code after 10 attempts");
            throw new AppException(AppError.UNCATEGORIZED_EXCEPTION);
        }

        registry.register(new LiveRoom(code, userId, mode));
        log.info("Room created roomCode={} hostId={} mode={}", code, userId, mode);
        eventPublisher.sendToUser(userId, "room:created", new RoomCreatedResponse(code, mode));
    }

    @Override
    public synchronized void joinRoom(Long userId, String rawCode) {
        String code = BattleRoomCode.normalize(rawCode);
        LiveRoom room = registry.getByCode(code)
                .orElseThrow(() -> new AppException(AppError.BATTLE_ROOM_NOT_FOUND));

        if (room.hostId().equals(userId)) {
            throw new AppException(AppError.BATTLE_ROOM_NOT_FOUND);
        }

        if (registry.isHosting(userId)) {
            throw new AppException(AppError.BATTLE_ALREADY_PARTICIPATED);
        }
        assertUserFree(userId);

        Long hostId = room.hostId();
        if (queueService.isUserInQueue(hostId) || matchService.isUserInActiveMatch(hostId)) {
            registry.removeByCode(code);
            throw new AppException(AppError.BATTLE_ROOM_NOT_FOUND);
        }

        registry.removeByCode(code);

        try {
            matchService.createRoomMatch(hostId, userId, room.mode(), room.roomCode());
            log.info("Room match launched roomCode={} hostId={} guestId={}", code, hostId, userId);
        } catch (AppException ex) {
            log.warn("Room match creation failed roomCode={} hostId={} guestId={} code={}",
                    code, hostId, userId, ex.getAppError().getCode());
            eventPublisher.sendError(hostId, ex.getAppError().getCode(), ex.getMessage());
            eventPublisher.sendError(userId, ex.getAppError().getCode(), ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error creating room match roomCode={} hostId={} guestId={}",
                    code, hostId, userId, ex);
            eventPublisher.sendError(hostId, "INTERNAL_ERROR", "Failed to create match");
            eventPublisher.sendError(userId, "INTERNAL_ERROR", "Failed to create match");
        }
    }

    @Override
    public synchronized void leaveRoom(Long userId) {
        registry.removeByHost(userId);
        eventPublisher.sendToUser(userId, "room:left", Collections.emptyMap());
        log.info("Room left userId={}", userId);
    }

    @Override
    public synchronized void onDisconnect(Long userId) {
        try {
            registry.removeByHost(userId).ifPresent(
                    r -> log.info("Host disconnected, room removed roomCode={} hostId={}", r.roomCode(), userId));
        } catch (Exception ex) {
            log.warn("Error cleaning up room on disconnect userId={}: {}", userId, ex.getMessage());
        }
    }
    
    // Đảm bảo user không tham gia hàng chờ hoặc trận đấu khác
    private void assertUserFree(Long userId) {
        if (queueService.isUserInQueue(userId) || matchService.isUserInActiveMatch(userId)) {
            throw new AppException(AppError.BATTLE_ALREADY_PARTICIPATED);
        }
    }
}

