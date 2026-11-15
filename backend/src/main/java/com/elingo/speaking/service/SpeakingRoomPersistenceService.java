package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import org.springframework.data.domain.Pageable;

public interface SpeakingRoomPersistenceService {
    SpeakingRoomDetailResponse createRoom(Long userId, String topic, Integer maxParticipants, Boolean isPrivate);
    SpeakingRoomDetailResponse joinRoom(Long userId, String roomCode);
    LeaveResult leaveRoom(Long userId, String roomCode);
    SpeakingRoomDetailResponse getRoomDetail(String roomCode);
    PageResponse<SpeakingRoomResponse> getOpenRooms(Pageable pageable);

    record LeaveResult(String roomCode, String topic, long durationSeconds,
                       Long newHostId,
                       boolean roomClosed) {}
}
