package com.elingo.speaking.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.speaking.config.SpeakingProperties;
import com.elingo.speaking.dto.request.SpeakingMicRequest;
import com.elingo.speaking.dto.request.SpeakingMuteRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.dto.response.MuteRequestedResponse;
import com.elingo.speaking.dto.response.PeerMicResponse;
import com.elingo.speaking.dto.response.RoomStateResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingSignalResponse;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;
import com.elingo.speaking.service.SpeakingSignalingService;
import com.elingo.speaking.state.LivePeer;
import com.elingo.speaking.state.LiveSpeakingRoom;
import com.elingo.speaking.state.SpeakingLiveRegistry;
import com.elingo.speaking.util.SpeakingRoomCode;
import com.elingo.speaking.websocket.SpeakingEventPublisher;
import com.elingo.speaking.websocket.SpeakingEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "SPEAKING-SIGNALING-SERVICE")
@Service
@RequiredArgsConstructor
public class SpeakingSignalingServiceImpl implements SpeakingSignalingService {

    private final SpeakingLiveRegistry registry;
    private final SpeakingEventPublisher publisher;
    private final SpeakingProperties props;

    @Override
    public void goOnline(Long userId, GoOnlineContext ctx) {
        String code = ctx.roomCode();

        // Nếu đang online ở phòng khác (do F5/phòng cũ) thì bỏ qua phòng cũ trong RAM
        LiveSpeakingRoom stale = registry.getByUser(userId);
        if (stale != null && !stale.getRoomCode().equals(code)) {
            removePeerSilently(stale, userId);
        }

        // Vòng lặp: lấy/tạo phòng và khóa. Nếu removeRoom chạy xen giữa thì tạo lại
        LiveSpeakingRoom room;
        while (true) {
            room = registry.getOrCreate(code, () -> new LiveSpeakingRoom(
                    ctx.roomCode(), ctx.roomId(), ctx.hostId(), ctx.topic(),
                    ctx.maxParticipants(), ctx.isPrivate(), ctx.startedAtMs()));
            room.lock();
            if (registry.get(code) == room) break;
            room.unlock();
        }
        try {
            boolean alreadyOnline = room.isOnline(userId);
            LivePeer peer = room.getPeer(userId);
            boolean wasInGrace = peer != null && !peer.isOnline() && room.cancelGrace(userId);

            // Làm mới từ DB (host, topic, trạng thái có thể đã đổi qua REST/WS khác)
            room.setHostId(ctx.hostId());
            room.setStartedAtMs(ctx.startedAtMs());
            room.setPrivate(ctx.isPrivate());

            if (peer == null) {
                peer = new LivePeer(userId, ctx.me().displayName(), ctx.me().avatarUrl(), ctx.me().role());
                room.putPeer(peer);
            } else {
                peer.setOnline(true);
                peer.setRole(ctx.me().role());
            }
            registry.bindUser(userId, code);

            // 1. trả room:state cho chính người gửi
            publisher.sendToUser(userId, SpeakingEvents.ROOM_STATE, buildState(room));

            // 2. thông báo cho người khác
            if (wasInGrace) {
                SpeakingPeerResponse reconnected = toPeer(peer);
                for (LivePeer other : room.onlinePeers()) {
                    if (!other.getUserId().equals(userId)) {
                        publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_RECONNECTED, reconnected);
                    }
                }
            } else if (!alreadyOnline) {
                SpeakingPeerResponse joined = toPeer(peer);
                for (LivePeer other : room.onlinePeers()) {
                    if (!other.getUserId().equals(userId)) {
                        publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_JOINED, joined);
                    }
                }
            }
            log.info("Speaking goOnline roomCode={} userId={} alreadyOnline={} wasInGrace={} online={}",
                    code, userId, alreadyOnline, wasInGrace, room.onlinePeers().size());
        } finally {
            room.unlock();
        }
    }

    @Override
    public void relaySignal(Long fromUserId, SpeakingSignalRequest req) {
        String code = SpeakingRoomCode.normalize(req.roomCode());
        LiveSpeakingRoom room = registry.get(code);

        // 1. null-check trước khi lock
        if (room == null || !room.isOnline(fromUserId)) {
            throw new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }

        room.lock();
        try {
            // re-check sau lock (state có thể đã đổi)
            if (!room.isOnline(fromUserId)) {
                throw new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED);
            }
            // 2. người nhận phải online và cùng phòng, khác người gửi
            if (req.toUserId().equals(fromUserId) || !room.isOnline(req.toUserId())) {
                throw new AppException(AppError.SPEAKING_SIGNAL_INVALID);
            }
            // 3. kích thước payload
            int size = req.payload().toString().length();
            if (req.payload().isNull() || size > props.signalMaxPayloadBytes()) {
                throw new AppException(AppError.SPEAKING_SIGNAL_INVALID);
            }
        } finally {
            room.unlock();
        }

        // fromUserId lấy từ Principal, không từ request
        publisher.sendToUser(req.toUserId(), SpeakingEvents.SIGNAL,
                new SpeakingSignalResponse(fromUserId, req.type(), req.payload()));
        log.debug("Speaking signal roomCode={} from={} to={} type={} size={}",
                code, fromUserId, req.toUserId(), req.type(),
                req.payload().toString().length());
    }

    @Override
    public void reportMic(Long userId, SpeakingMicRequest req) {
        LiveSpeakingRoom room = registry.get(SpeakingRoomCode.normalize(req.roomCode()));
        if (room == null) {
            throw new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED);
        }
        room.lock();
        try {
            LivePeer me = room.getPeer(userId);
            if (me == null || !me.isOnline()) {
                throw new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED);
            }
            if (me.isMicMuted() == req.muted()) return; // không đổi -> không phát (tránh lặp)
            me.setMicMuted(req.muted());
            for (LivePeer other : room.onlinePeers()) {
                if (!other.getUserId().equals(userId)) {
                    publisher.sendToUser(other.getUserId(), SpeakingEvents.PEER_MIC, new PeerMicResponse(userId, req.muted()));
                }
            }
        } finally {
            room.unlock();
        }
    }

    @Override
    public void requestMute(Long hostId, SpeakingMuteRequest req) {
        LiveSpeakingRoom room = registry.get(SpeakingRoomCode.normalize(req.roomCode()));
        if (room == null) throw new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED);
        room.lock();
        try {
            if (!room.isOnline(hostId)) {
                throw new AppException(AppError.SPEAKING_ROOM_ACCESS_DENIED);
            }
            if (!hostId.equals(room.getHostId())) {
                throw new AppException(AppError.SPEAKING_NOT_HOST);
            }
            if (hostId.equals(req.targetUserId())) {
                throw new AppException(AppError.INVALID_REQUEST); // tự tắt mic mình dùng nút mic thường
            }
            if (!room.isOnline(req.targetUserId())) {
                throw new AppException(AppError.SPEAKING_PARTICIPANT_NOT_FOUND);
            }

            publisher.sendToUser(req.targetUserId(), SpeakingEvents.MUTE_REQUESTED, new MuteRequestedResponse(hostId));
        } finally {
            room.unlock();
        }
    }

    /**
     * Private helper methods
     */
    private RoomStateResponse buildState(LiveSpeakingRoom room) {
        return new RoomStateResponse(
                room.getRoomCode(), room.getTopic(), room.getHostId(),
                room.getMaxParticipants(), room.isPrivate(),
                room.getStartedAtMs(), System.currentTimeMillis(),
                room.onlinePeers().stream().map(this::toPeer).toList(),
                room.onlinePeers().stream().filter(LivePeer::isMicMuted).map(LivePeer::getUserId).toList());
    }

    private SpeakingPeerResponse toPeer(LivePeer p) {
        return new SpeakingPeerResponse(p.getUserId(), p.getDisplayName(), p.getAvatarUrl(), p.getRole());
    }

    // Xóa peer khỏi phòng cũ trong RAM mà không phát event (phòng cũ đã không còn liên quan)
    private void removePeerSilently(LiveSpeakingRoom stale, Long userId) {
        stale.lock();
        try {
            stale.removePeer(userId);
            registry.unbindUser(userId, stale.getRoomCode());
            if (stale.isEmpty()) registry.removeRoom(stale);
        } finally {
            stale.unlock();
        }
    }
}
