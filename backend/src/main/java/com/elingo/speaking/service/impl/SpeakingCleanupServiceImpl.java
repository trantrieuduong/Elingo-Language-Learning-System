package com.elingo.speaking.service.impl;

import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.service.SpeakingCleanupService;
import com.elingo.speaking.service.SpeakingRoomPersistenceService;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j(topic = "SPEAKING-CLEANUP-SERVICE")
@Service
@RequiredArgsConstructor
public class SpeakingCleanupServiceImpl implements SpeakingCleanupService {

    private final SpeakingRoomPersistenceService persistence;
    private final SpeakingLiveRegistry registry;
    private final SpeakingProperties props;

    @Override
    public int cleanStaleRooms() {
        LocalDateTime before = LocalDateTime.now().minus(Duration.ofMillis(props.waitingRoomTtlMs()));
        int closed = 0;
        for (String code : persistence.findStaleRoomCodes(before)) {
            if (hasAnyoneLive(code)) continue; // còn người online hoặc đang trong grace -> giữ
            if (persistence.forceCloseRoom(code)) {
                closed++;
                dropLiveRoom(code);
                log.info("closed stale room roomCode={}", code);
            }
        }
        return closed;
    }

    @Override
    public int cleanAllOnStartup() {
        int n = persistence.closeAllOpenRooms();
        if (n > 0) log.warn("closed {} orphan speaking rooms on startup", n);
        return n;
    }

    // true nếu RAM còn phòng có peer (online hoặc đang chờ grace)
    private boolean hasAnyoneLive(String code) {
        LiveSpeakingRoom room = registry.get(code);
        if (room == null) return false;
        room.lock();
        try {
            return !room.isEmpty();
        } finally {
            room.unlock();
        }
    }

    private void dropLiveRoom(String code) {
        LiveSpeakingRoom room = registry.get(code);
        if (room == null) return;
        room.lock();
        try {
            room.cancelAllGraces();
            registry.removeRoom(room);
        } finally {
            room.unlock();
        }
    }
}
