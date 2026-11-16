package com.elingo.speaking.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry RAM toàn cục: roomCode -> LiveSpeakingRoom, userId -> roomCode
 */
@Component
public class SpeakingLiveRegistry {

    private final Map<String, LiveSpeakingRoom> rooms = new ConcurrentHashMap<>();
    private final Map<Long, String> codeByUser = new ConcurrentHashMap<>();   // chỉ người đã join (online)

    public LiveSpeakingRoom getOrCreate(String code, Supplier<LiveSpeakingRoom> factory) {
        return rooms.computeIfAbsent(code, k -> factory.get());
    }

    public LiveSpeakingRoom get(String code) {
        return rooms.get(code);
    }

    public LiveSpeakingRoom getByUser(Long userId) {
        String c = codeByUser.get(userId);
        return c == null ? null : rooms.get(c);
    }

    public void bindUser(Long userId, String code) {
        codeByUser.put(userId, code);
    }

    public void unbindUser(Long userId, String code) {
        codeByUser.remove(userId, code);
    }

    public void removeRoom(LiveSpeakingRoom r) {
        rooms.remove(r.getRoomCode());
        r.getPeers().keySet().forEach(u -> codeByUser.remove(u, r.getRoomCode()));
    }
}
