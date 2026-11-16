package com.elingo.speaking.service.impl;

import com.elingo.speaking.dto.response.PeerLeftResponse;
import com.elingo.speaking.service.SpeakingConnectionService;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "SPEAKING-CONNECTION-SERVICE")
@Service
@RequiredArgsConstructor
public class SpeakingConnectionServiceImpl implements SpeakingConnectionService {

    private final SpeakingLiveRegistry registry;
    private final SpeakingEventPublisher publisher;

    @Override
    public void onUserOffline(Long userId) {
        LiveSpeakingRoom room = registry.getByUser(userId);
        if (room == null) return;
        room.lock();
        try {
            LivePeer peer = room.getPeer(userId);
            if (peer == null || !peer.isOnline()) return;
            room.removePeer(userId);
            registry.unbindUser(userId, room.getRoomCode());
            for (LivePeer other : room.onlinePeers()) {
                publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_LEFT,
                        new PeerLeftResponse(userId, "DISCONNECTED"));
            }
            if (room.isEmpty()) registry.removeRoom(room);
            log.info("Speaking user offline roomCode={} userId={}", room.getRoomCode(), userId);
        } finally {
            room.unlock();
        }
    }
}
