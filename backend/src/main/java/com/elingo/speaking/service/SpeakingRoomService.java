package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface SpeakingRoomService {
    SpeakingRoomDetailResponse createRoom(Long userId, CreateSpeakingRoomRequest request);
    SpeakingRoomDetailResponse joinRoom(Long userId, String roomCode);
    SpeakingSessionSummaryResponse leaveRoom(Long userId, String roomCode);
    SpeakingRoomDetailResponse getRoom(String roomCode);
    PageResponse<SpeakingRoomResponse> getOpenRooms(Pageable pageable);
}
