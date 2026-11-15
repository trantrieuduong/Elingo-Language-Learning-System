package com.elingo.speaking.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import com.elingo.speaking.service.SpeakingRoomPersistenceService;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.LeaveResult;
import com.elingo.speaking.service.SpeakingRoomService;
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
        LeaveResult r = persistence.leaveRoom(userId, roomCode);
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
