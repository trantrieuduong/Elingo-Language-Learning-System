package com.elingo.speaking.service.impl;

import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.response.PeerDisconnectedResponse;
import com.elingo.speaking.event.SpeakingMemberRemovedEvent;
import com.elingo.speaking.service.SpeakingConnectionService;
import com.elingo.speaking.service.SpeakingRoomService;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j(topic = "SPEAKING-CONNECTION-SERVICE")
@Service
public class SpeakingConnectionServiceImpl implements SpeakingConnectionService {

    private final SpeakingLiveRegistry registry;
    private final SpeakingEventPublisher publisher;
    private final SpeakingRoomService roomService;
    private final SpeakingProperties props;
    private final ThreadPoolTaskScheduler speakingScheduler;

    public SpeakingConnectionServiceImpl(
            SpeakingLiveRegistry registry,
            SpeakingEventPublisher publisher,
            SpeakingRoomService roomService,
            SpeakingProperties props,
            @Qualifier("speakingScheduler") ThreadPoolTaskScheduler speakingScheduler
    ) {
        this.registry = registry;
        this.publisher = publisher;
        this.roomService = roomService;
        this.props = props;
        this.speakingScheduler = speakingScheduler;
    }

    @Override
    public void onUserOffline(Long userId) {
        LiveSpeakingRoom room = registry.getByUser(userId);
        if (room == null) return;
        room.lock();
        try {
            LivePeer peer = room.getPeer(userId);
            if (peer == null || !peer.isOnline()) return; // đã ngắt rồi (tránh lặp)
            peer.setOnline(false);
            boolean isHost = userId.equals(room.getHostId());
            long graceMs = isHost ? props.hostReconnectGraceMs() : props.reconnectGraceMs();
            for (LivePeer other : room.onlinePeers()) {
                publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_DISCONNECTED,
                        new PeerDisconnectedResponse(userId, graceMs));
            }
            String code = room.getRoomCode();
            room.putGrace(userId, speakingScheduler.schedule(
                    () -> onGraceExpired(code, userId),
                    Instant.now().plusMillis(graceMs)));
            // host offline mà không còn ai khác trong phòng -> rút ngắn thời gian chờ ngay
            shortenHostGraceIfAlone(room);
            log.info("Speaking user disconnected roomCode={} userId={} host={} graceMs={}", code, userId, isHost, graceMs);
        } finally {
            room.unlock();
        }
    }

    private void onGraceExpired(String roomCode, Long userId) {
        try {
            LiveSpeakingRoom room = registry.get(roomCode);
            if (room == null) return;
            room.lock();
            try {
                LivePeer peer = room.getPeer(userId);
                if (peer == null || peer.isOnline()) return; // đã quay lại / đã rời
            } finally {
                room.unlock();
            }
            roomService.removeMember(userId, roomCode, "TIMEOUT"); // không giữ lock khi gọi DB
        } catch (Exception ex) {
            log.error("Grace expiry failed roomCode={} userId={}", roomCode, userId, ex);
        }
    }

    // Gọi sau mỗi lần có người bị gỡ khỏi phòng
    @EventListener
    public void onMemberRemoved(SpeakingMemberRemovedEvent ev) {
        LiveSpeakingRoom room = registry.get(ev.roomCode());
        if (room != null) shortenHostGraceIfAlone(room);
    }

    // Nếu chỉ còn đúng host trong phòng và host đang offline -> rút ngắn thời gian chờ
    private void shortenHostGraceIfAlone(LiveSpeakingRoom room) {
        room.lock();
        try {
            Long hostId = room.getHostId();
            LivePeer host = hostId == null ? null : room.getPeer(hostId);
            if (host == null || host.isOnline() || room.getPeers().size() != 1) return; // chỉ còn đúng mình host và host đang offline
            String code = room.getRoomCode();
            room.putGrace(hostId, speakingScheduler.schedule( // putGrace tự hủy future cũ
                    () -> onGraceExpired(code, hostId),
                    Instant.now().plusMillis(props.reconnectGraceMs())));
            log.info("Host grace shortened roomCode={} hostId={} graceMs={}", code, hostId, props.reconnectGraceMs());
        } finally {
            room.unlock();
        }
    }
}
