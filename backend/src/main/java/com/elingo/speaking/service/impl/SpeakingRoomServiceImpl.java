package com.elingo.speaking.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.response.HostChangedResponse;
import com.elingo.speaking.dto.response.PeerLeftResponse;
import com.elingo.speaking.dto.response.RoomClosedResponse;
import com.elingo.speaking.dto.response.RoomCreatedResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.event.SpeakingMemberRemovedEvent;
import com.elingo.speaking.service.SpeakingRoomPersistenceService;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.LeaveResult;
import com.elingo.speaking.service.SpeakingRoomService;
import com.elingo.speaking.service.SpeakingSignalingService;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final SpeakingSignalingService signalingService;
    private final ApplicationEventPublisher eventPublisher;

    // Khóa đồng bộ theo user, tránh tạo hoặc tham gia nhiều phòng đồng thời
    private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    @Override
    public void createRoom(Long userId, CreateSpeakingRoomRequest request) {
        SpeakingRoomDetailResponse room;
        synchronized (userLocks.computeIfAbsent(userId, k -> new Object())) {
            room = persistence.createRoom(userId, request.topic(), request.maxParticipants(), request.isPrivate());
        }
        publisher.sendToUser(userId, SpeakingEvents.ROOM_CREATED, new RoomCreatedResponse(room.roomCode()));
    }

    @Override
    public void joinRoom(Long userId, String roomCode) {
        GoOnlineContext ctx;
        synchronized (userLocks.computeIfAbsent(userId, k -> new Object())) {
            ctx = persistence.joinRoom(userId, roomCode); // DB; idempotent nếu đã là thành viên
        }
        signalingService.goOnline(userId, ctx); // RAM + room:state + peer:joined
    }

    @Override
    public void leaveRoom(Long userId) {
        String roomCode = persistence.findActiveRoomCode(userId);
        LeaveResult r = persistence.leaveRoom(userId, roomCode);
        afterLeave(userId, r, "LEFT");
        publisher.sendToUser(userId, SpeakingEvents.ROOM_LEFT,
                new SpeakingSessionSummaryResponse(r.roomCode(), r.topic(), r.durationSeconds()));
    }

    @Override
    public void removeMember(Long userId, String roomCode, String reason) {
        try {
            LeaveResult r = persistence.leaveRoom(userId, roomCode);
            afterLeave(userId, r, reason);
        } catch (AppException ex) {
            // đã rời / phòng không còn -> bỏ qua
            log.debug("removeMember skipped roomCode={} userId={} code={}", roomCode, userId, ex.getAppError().getCode());
        }
    }

    private void afterLeave(Long userId, LeaveResult r, String reason) {
        LiveSpeakingRoom room = registry.get(r.roomCode());
        if (room == null) return;
        room.lock();
        try {
            room.cancelGrace(userId); // hủy đồng hồ chờ (nếu người này đang trong grace)
            room.removePeer(userId);
            registry.unbindUser(userId, r.roomCode());
            if (r.newHostId() != null) room.setHostId(r.newHostId());

            for (LivePeer other : room.onlinePeers()) {
                publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_LEFT, new PeerLeftResponse(userId, reason));
                if (r.newHostId() != null) {
                    publisher.sendToUser(other.getUserId(), SpeakingEvents.HOST_CHANGED, new HostChangedResponse(r.newHostId()));
                }
                if (r.roomClosed()) {
                    publisher.sendToUser(other.getUserId(), SpeakingEvents.ROOM_CLOSED, new RoomClosedResponse(r.roomCode(), "EMPTY"));
                }
            }
            // cập nhật role trong RAM cho host mới
            if (r.newHostId() != null) {
                LivePeer np = room.getPeer(r.newHostId());
                if (np != null) np.setRole(ParticipantRole.HOST);
            }
            if (r.roomClosed() || room.isEmpty()) {
                room.cancelAllGraces();
                registry.removeRoom(room);
            }
        } finally {
            room.unlock();
        }
        if (!r.roomClosed()) {
            eventPublisher.publishEvent(new SpeakingMemberRemovedEvent(r.roomCode()));
        }
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
