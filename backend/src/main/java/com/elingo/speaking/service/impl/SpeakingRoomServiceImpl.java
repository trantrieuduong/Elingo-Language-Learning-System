package com.elingo.speaking.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.ActiveRoomResponse;
import com.elingo.speaking.dto.response.BanRemovedResponse;
import com.elingo.speaking.dto.response.HostChangedResponse;
import com.elingo.speaking.dto.response.PeerLeftResponse;
import com.elingo.speaking.dto.response.RoomClosedResponse;
import com.elingo.speaking.dto.response.RoomCreatedResponse;
import com.elingo.speaking.dto.response.RoomKickedResponse;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import com.elingo.speaking.entity.ParticipantRole;
import com.elingo.speaking.event.SpeakingMemberRemovedEvent;
import com.elingo.speaking.service.SpeakingRoomPersistenceService;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.EndResult;
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

import java.util.List;
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

    @Override
    public void transferHost(Long userId, String roomCode, Long newHostId) {
        SpeakingRoomDetailResponse detail = persistence.transferHost(userId, roomCode, newHostId);
        LiveSpeakingRoom room = registry.get(detail.roomCode());
        if (room != null) {
            room.lock();
            try {
                room.setHostId(newHostId);
                LivePeer oldHost = room.getPeer(userId);
                if (oldHost != null) oldHost.setRole(ParticipantRole.GUEST);
                LivePeer newHost = room.getPeer(newHostId);
                if (newHost != null) newHost.setRole(ParticipantRole.HOST);
                for (LivePeer p : room.onlinePeers()) { // gồm cả người chuyển
                    publisher.sendToUser(p.getUserId(), SpeakingEvents.HOST_CHANGED, new HostChangedResponse(newHostId));
                }
            } finally {
                room.unlock();
            }
        }
    }

    @Override
    public void endRoom(Long userId, String roomCode) {
        EndResult r = persistence.endRoom(userId, roomCode);
        LiveSpeakingRoom room = registry.get(r.roomCode());
        if (room != null) {
            room.lock();
            try {
                for (LivePeer p : room.getPeers().values()) {
                    if (!p.getUserId().equals(userId)) { // host nhận room:left bên dưới
                        if (p.isOnline()) {
                            publisher.sendToUser(p.getUserId(), SpeakingEvents.ROOM_CLOSED,
                                    new RoomClosedResponse(r.roomCode(), "ENDED_BY_HOST"));
                        }
                        clearActiveRoom(p.getUserId());
                    }
                }
                room.cancelAllGraces(); // người đang chờ quay lại sẽ nhận lỗi khi join
                registry.unbindUser(userId, r.roomCode()); // host không còn thuộc phòng trong RAM
                registry.removeRoom(room);
            } finally {
                room.unlock();
            }
        }
        clearActiveRoom(userId);
        publisher.sendToUser(userId, SpeakingEvents.ROOM_LEFT,
                new SpeakingSessionSummaryResponse(r.roomCode(), r.topic(), r.durationSeconds()));
    }

    @Override
    public void kickMember(Long hostId, String roomCode, Long targetUserId) {
        SpeakingRoomDetailResponse detail = persistence.kickMember(hostId, roomCode, targetUserId);
        LiveSpeakingRoom room = registry.get(detail.roomCode());
        if (room != null) {
            room.lock();
            try {
                LivePeer target = room.getPeer(targetUserId);
                if (target != null) {
                    room.cancelGrace(targetUserId); // hủy cả thời gian chờ quay lại
                    if (target.isOnline()) {
                        publisher.sendToUser(targetUserId, SpeakingEvents.ROOM_KICKED, new RoomKickedResponse(detail.roomCode()));
                    }
                    clearActiveRoom(targetUserId);
                    room.removePeer(targetUserId);
                    registry.unbindUser(targetUserId, detail.roomCode());
                    for (LivePeer other : room.onlinePeers()) { // gồm cả host
                        publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_LEFT,
                                new PeerLeftResponse(targetUserId, "KICKED"));
                    }
                }
            } finally {
                room.unlock();
            }
            eventPublisher.publishEvent(new SpeakingMemberRemovedEvent(detail.roomCode()));
        }
    }

    @Override
    public List<SpeakingBannedUserResponse> listBans(Long hostId, String roomCode) {
        return persistence.listBans(hostId, roomCode);
    }

    @Override
    public void unbanMember(Long hostId, String roomCode, Long targetUserId) {
        persistence.unbanMember(hostId, roomCode, targetUserId);
        publisher.sendToUser(hostId, SpeakingEvents.BAN_REMOVED, new BanRemovedResponse(targetUserId)); // chỉ host nhận
    }

    private void afterLeave(Long userId, LeaveResult r, String reason) {
        LiveSpeakingRoom room = registry.get(r.roomCode());
        if (room == null) return;
        room.lock();
        try {
            room.cancelGrace(userId); // hủy đồng hồ chờ (nếu người này đang trong grace)
            room.removePeer(userId);
            registry.unbindUser(userId, r.roomCode());
            if ("TIMEOUT".equals(reason)) {
                clearActiveRoom(userId);
            }
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

    private void clearActiveRoom(Long userId) {
        publisher.sendToUser(userId, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(null));
    }

    @Override
    public SpeakingRoomDetailResponse getRoom(String roomCode) {
        return persistence.getRoomDetail(roomCode);
    }

    @Override
    public void sendActiveRoom(Long userId) {
        String code = persistence.findActiveRoomCodeOrNull(userId);
        publisher.sendToUser(userId, SpeakingEvents.ROOM_ACTIVE, new ActiveRoomResponse(code));
    }

    @Override
    public PageResponse<SpeakingRoomResponse> getOpenRooms(SpeakingRoomSearchRequest request, Pageable pageable) {
        return persistence.getOpenRooms(request, pageable);
    }
}
