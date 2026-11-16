package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import org.springframework.data.domain.Pageable;

public interface SpeakingRoomService {
    void createRoom(Long userId, CreateSpeakingRoomRequest request);
    void joinRoom(Long userId, String roomCode);
    void leaveRoom(Long userId);
    void removeMember(Long userId, String roomCode, String reason);
    SpeakingRoomDetailResponse getRoom(String roomCode);
    PageResponse<SpeakingRoomResponse> getOpenRooms(Pageable pageable);
}
