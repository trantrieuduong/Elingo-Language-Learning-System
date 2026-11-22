package com.elingo.speaking.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.SpeakingBannedUserResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpeakingRoomService {
    void createRoom(Long userId, CreateSpeakingRoomRequest request);
    void joinRoom(Long userId, String roomCode);
    void leaveRoom(Long userId);
    void removeMember(Long userId, String roomCode, String reason);
    void transferHost(Long userId, String roomCode, Long newHostId);
    void endRoom(Long userId, String roomCode);
    void kickMember(Long hostId, String roomCode, Long targetUserId);
    List<SpeakingBannedUserResponse> listBans(Long hostId, String roomCode);
    void unbanMember(Long hostId, String roomCode, Long targetUserId);
    SpeakingRoomDetailResponse getRoom(String roomCode);
    void sendActiveRoom(Long userId);
    PageResponse<SpeakingRoomResponse> getOpenRooms(SpeakingRoomSearchRequest request, Pageable pageable);
}
