package com.elingo.speaking.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.response.PeerLeftResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.service.SpeakingRoomPersistenceService;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.LeaveResult;
import com.elingo.speaking.service.SpeakingRoomService;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j(topic = "SPEAKING-ROOM-SERVICE")
@Service
@RequiredArgsConstructor
public class SpeakingRoomServiceImpl implements SpeakingRoomService {

    private final SpeakingRoomPersistenceService persistence;
    private final SpeakingLiveRegistry registry;
    private final SpeakingEventPublisher publisher;

    // Khóa đồng bộ theo user, tránh tạo hoặc tham gia nhiều phòng đồng thời
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    @Override
    public SpeakingRoomDetailResponse createRoom(Long userId, CreateSpeakingRoomRequest request) {
        synchronized (userLocks.computeIfAbsent(userId, k -> new Object())) {
            return persistence.createRoom(userId, request.topic(), request.maxParticipants(), request.isPrivate());
        }
    }

    @Override
    public SpeakingRoomDetailResponse joinRoom(Long userId, String roomCode) {
        synchronized (userLocks.computeIfAbsent(userId, k -> new Object())) {
            return persistence.joinRoom(userId, roomCode);
        }
    }

    @Override
    public SpeakingSessionSummaryResponse leaveRoom(Long userId, String roomCode) {
        LeaveResult r = persistence.leaveRoom(userId, roomCode);   // đã commit

        LiveSpeakingRoom room = registry.get(r.roomCode());
        if (room != null) {
            room.lock();
            try {
                room.removePeer(userId);
                registry.unbindUser(userId, r.roomCode());
                if (r.newHostId() != null) {
                    room.setHostId(r.newHostId());
                    // Cập nhật role peer mới thành HOST trong RAM
                    LivePeer newHost = room.getPeer(r.newHostId());
                    if (newHost != null) newHost.setRole(ParticipantRole.HOST);
                }
                for (LivePeer other : room.onlinePeers()) {
                    publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_LEFT,
                            new PeerLeftResponse(userId, "LEFT"));
                }
                if (r.roomClosed() || room.isEmpty()) registry.removeRoom(room);
            } finally {
                room.unlock();
            }
        }

        return new SpeakingSessionSummaryResponse(r.roomCode(), r.topic(), r.durationSeconds());
    }

    @Override
    public SpeakingRoomDetailResponse getRoom(String roomCode) {
        return persistence.getRoomDetail(roomCode);
    }

    @Override
    public PageResponse<SpeakingRoomResponse> getOpenRooms(Pageable pageable) {
        return persistence.getOpenRooms(pageable);
    }
}
