package com.elingo.battle.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BattleRoomRegistry {
    
    private final Map<String, LiveRoom> byCode = new ConcurrentHashMap<>();
    private final Map<Long, String> codeByHost = new ConcurrentHashMap<>();

    public void register(LiveRoom room) {
        byCode.put(room.roomCode(), room);
        codeByHost.put(room.hostId(), room.roomCode());
    }

    public Optional<LiveRoom> getByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public boolean codeExists(String code) {
        return byCode.containsKey(code);
    }

    public boolean isHosting(Long userId) {
        return codeByHost.containsKey(userId);
    }

    public Optional<LiveRoom> removeByHost(Long hostId) {
        String code = codeByHost.remove(hostId);
        if (code != null) {
            LiveRoom removed = byCode.remove(code);
            return Optional.ofNullable(removed);
        }
        return Optional.empty();
    }

    public Optional<LiveRoom> removeByCode(String code) {
        LiveRoom removed = byCode.remove(code);
        if (removed != null) {
            codeByHost.remove(removed.hostId());
            return Optional.of(removed);
        }
        return Optional.empty();
    }
}
