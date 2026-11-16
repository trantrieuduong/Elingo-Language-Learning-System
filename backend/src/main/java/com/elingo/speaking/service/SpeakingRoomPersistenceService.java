package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.response.SpeakingPeerResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpeakingRoomPersistenceService {
    SpeakingRoomDetailResponse createRoom(Long userId, String topic, Integer maxParticipants, Boolean isPrivate);
    SpeakingRoomDetailResponse joinRoom(Long userId, String roomCode);
    LeaveResult leaveRoom(Long userId, String roomCode);
    SpeakingRoomDetailResponse getRoomDetail(String roomCode);
    PageResponse<SpeakingRoomResponse> getOpenRooms(Pageable pageable);
    EnterContext loadRoomForEnter(Long userId, String roomCode);

    record LeaveResult(String roomCode, String topic, long durationSeconds,
                       Long newHostId,
                       boolean roomClosed) {}

    record EnterContext(Long roomId, String roomCode, String topic, Long hostId,
                        Short maxParticipants, Long startedAtMs,
                        SpeakingPeerResponse me, List<SpeakingPeerResponse> members) {}
}
