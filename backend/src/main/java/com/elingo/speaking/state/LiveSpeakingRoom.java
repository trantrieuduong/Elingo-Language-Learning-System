package com.elingo.speaking.state;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Trạng thái RAM của một phòng
 * Mọi thay đổi phải trong lock()/unlock()
 */
@Getter
public class LiveSpeakingRoom {
    private final String roomCode;
    private final Long roomId;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<Long, LivePeer> peers = new LinkedHashMap<>();
    private final Map<Long, ScheduledFuture<?>> graceFutures = new HashMap<>();
    @Setter private volatile Long hostId;
    @Setter private volatile String topic;
    @Setter private volatile Short maxParticipants;
    @Setter private volatile boolean isPrivate;
    @Setter private volatile Long startedAtMs;

    public LiveSpeakingRoom(String roomCode, Long roomId, Long hostId,
                            String topic, Short maxParticipants, boolean isPrivate, Long startedAtMs) {
        this.roomCode = roomCode;
        this.roomId = roomId;
        this.hostId = hostId;
        this.topic = topic;
        this.maxParticipants = maxParticipants;
        this.isPrivate = isPrivate;
        this.startedAtMs = startedAtMs;
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public LivePeer getPeer(Long userId) {
        return peers.get(userId);
    }

    public boolean isOnline(Long userId) {
        LivePeer p = peers.get(userId);
        return p != null && p.isOnline();
    }

    public List<LivePeer> onlinePeers() {
        return peers.values().stream().filter(LivePeer::isOnline).toList();
    }

    public void putPeer(LivePeer p) {
        peers.put(p.getUserId(), p);
    }

    public LivePeer removePeer(Long userId) {
        return peers.remove(userId);
    }

    public boolean isEmpty() {
        return peers.isEmpty();
    }

    public void putGrace(Long userId, ScheduledFuture<?> f) {
        cancelGrace(userId);
        graceFutures.put(userId, f);
    }

    public boolean cancelGrace(Long userId) {
        ScheduledFuture<?> f = graceFutures.remove(userId);
        if (f != null) {
            f.cancel(false);
            return true;
        }
        return false;
    }

    public void cancelAllGraces() {
        graceFutures.values().forEach(f -> f.cancel(false));
        graceFutures.clear();
    }
}
