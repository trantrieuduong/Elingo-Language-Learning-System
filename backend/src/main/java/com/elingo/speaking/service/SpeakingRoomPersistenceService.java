package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface SpeakingRoomPersistenceService {
    SpeakingRoomDetailResponse createRoom(Long userId, String topic, Integer maxParticipants, Boolean isPrivate);
    GoOnlineContext joinRoom(Long userId, String roomCode);
    LeaveResult leaveRoom(Long userId, String roomCode);
    SpeakingRoomDetailResponse transferHost(Long userId, String roomCode, Long newHostId);
    EndResult endRoom(Long userId, String roomCode);
    SpeakingRoomDetailResponse kickMember(Long hostId, String roomCode, Long targetUserId);
    List<SpeakingBannedUserResponse> listBans(Long hostId, String roomCode);
    List<SpeakingBannedUserResponse> unbanMember(Long hostId, String roomCode, Long targetUserId);
    List<String> findStaleRoomCodes(LocalDateTime before);
    boolean forceCloseRoom(String roomCode);
    int closeAllOpenRooms();
    String findActiveRoomCode(Long userId);
    String findActiveRoomCodeOrNull(Long userId);
    SpeakingRoomDetailResponse getRoomDetail(String roomCode);
    PageResponse<SpeakingRoomResponse> getOpenRooms(SpeakingRoomSearchRequest request, Pageable pageable);

    record LeaveResult(String roomCode, String topic, long durationSeconds,
                       Long newHostId,
                       boolean roomClosed) {}

    record EndResult(String roomCode, String topic, long durationSeconds) {}

    record GoOnlineContext(Long roomId, String roomCode, String topic, Long hostId,
                           Short maxParticipants, boolean isPrivate, Long startedAtMs,
                           SpeakingPeerResponse me, List<SpeakingPeerResponse> members) {}
}
