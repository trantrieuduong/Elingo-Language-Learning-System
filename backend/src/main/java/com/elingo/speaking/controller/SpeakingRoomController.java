package com.elingo.speaking.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.JoinSpeakingRoomRequest;
import com.elingo.speaking.dto.response.IceConfigResponse;
import com.elingo.speaking.dto.response.SpeakingRoomDetailResponse;
import com.elingo.speaking.dto.response.SpeakingRoomResponse;
import com.elingo.speaking.dto.response.SpeakingSessionSummaryResponse;
import com.elingo.speaking.service.SpeakingIceService;
import com.elingo.speaking.service.SpeakingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/speaking-rooms")
@RequiredArgsConstructor
@Tag(name = "Speaking Room Controller", description = "Speaking room create/join/leave, lobby and detail APIs")
public class SpeakingRoomController {

    private final SpeakingRoomService speakingRoomService;
    private final SpeakingIceService iceService;

    @PostMapping
    @Operation(summary = "Create a speaking room; the caller becomes HOST")
    public ApiResponse<SpeakingRoomDetailResponse> create(@CurrentUserId Long userId,
            @RequestBody @Valid CreateSpeakingRoomRequest request) {
        SpeakingRoomDetailResponse data = speakingRoomService.createRoom(userId, request);
        return ApiResponse.<SpeakingRoomDetailResponse>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping
    @Operation(summary = "List open speaking rooms (WAITING/ACTIVE and not full)")
    public ApiResponse<PageResponse<SpeakingRoomResponse>> list(@PageableDefault(size = 10) Pageable pageable) {
        PageResponse<SpeakingRoomResponse> data = speakingRoomService.getOpenRooms(pageable);
        return ApiResponse.<PageResponse<SpeakingRoomResponse>>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping("/ice-config")
    @Operation(summary = "Get STUN/TURN servers for WebRTC peer connections")
    public ApiResponse<IceConfigResponse> iceConfig(@CurrentUserId Long userId) {
        IceConfigResponse data = iceService.getIceConfig(userId);
        return ApiResponse.<IceConfigResponse>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping("/{roomCode}")
    @Operation(summary = "Get speaking room detail with current participants")
    public ApiResponse<SpeakingRoomDetailResponse> detail(@PathVariable String roomCode) {
        SpeakingRoomDetailResponse data = speakingRoomService.getRoom(roomCode);
        return ApiResponse.<SpeakingRoomDetailResponse>builder()
                .success(true)
                .data(data)
                .build();
    }

    @PostMapping("/join")
    @Operation(summary = "Join a speaking room by code (idempotent if already in it)")
    public ApiResponse<SpeakingRoomDetailResponse> join(@CurrentUserId Long userId,
            @RequestBody @Valid JoinSpeakingRoomRequest request) {
        SpeakingRoomDetailResponse data = speakingRoomService.joinRoom(userId, request.roomCode());
        return ApiResponse.<SpeakingRoomDetailResponse>builder()
                .success(true)
                .data(data)
                .build();
    }

    @PostMapping("/{roomCode}/leave")
    @Operation(summary = "Leave a speaking room")
    public ApiResponse<SpeakingSessionSummaryResponse> leave(@CurrentUserId Long userId,
            @PathVariable String roomCode) {
        SpeakingSessionSummaryResponse data = speakingRoomService.leaveRoom(userId, roomCode);
        return ApiResponse.<SpeakingSessionSummaryResponse>builder()
                .success(true)
                .data(data)
                .build();
    }
}
