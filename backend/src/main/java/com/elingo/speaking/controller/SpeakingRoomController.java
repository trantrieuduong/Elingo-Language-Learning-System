package com.elingo.speaking.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.speaking.dto.request.SpeakingRoomSearchRequest;
import com.elingo.speaking.dto.response.*;
import com.elingo.speaking.service.SpeakingHistoryService;
import com.elingo.speaking.service.SpeakingIceService;
import com.elingo.speaking.service.SpeakingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/speaking-rooms")
@RequiredArgsConstructor
@Tag(name = "Speaking Room Controller",
        description = "Speaking lobby listing, room detail and ICE config (create/join/leave go through WebSocket)")
public class SpeakingRoomController {

    private final SpeakingRoomService speakingRoomService;
    private final SpeakingIceService iceService;
    private final SpeakingHistoryService historyService;

    @GetMapping
    @Operation(summary = "List open speaking rooms (WAITING/ACTIVE and not full) with optional keyword and capacity filter")
    public ApiResponse<PageResponse<SpeakingRoomResponse>> list(
            @ModelAttribute @Valid SpeakingRoomSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<SpeakingRoomResponse> data = speakingRoomService.getOpenRooms(request, pageable);
        return ApiResponse.<PageResponse<SpeakingRoomResponse>>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping("/history")
    @Operation(summary = "My speaking history grouped by room, newest first (filters: keyword, maxParticipants)")
    public ApiResponse<PageResponse<SpeakingHistoryItemResponse>> history(
            @CurrentUserId Long userId,
            @ModelAttribute @Valid SpeakingRoomSearchRequest request,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<SpeakingHistoryItemResponse> data = historyService.getHistory(userId, request, pageable);
        return ApiResponse.<PageResponse<SpeakingHistoryItemResponse>>builder()
                .success(true)
                .data(data)
                .build();
    }

    @GetMapping("/history/summary")
    @Operation(summary = "My total speaking time, rooms and sessions")
    public ApiResponse<SpeakingHistorySummaryResponse> historySummary(@CurrentUserId Long userId) {
        SpeakingHistorySummaryResponse data = historyService.getSummary(userId);
        return ApiResponse.<SpeakingHistorySummaryResponse>builder()
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

    @GetMapping("/{roomCode}/bans")
    @Operation(summary = "Host views the kicked list of the room")
    public ApiResponse<List<SpeakingBannedUserResponse>> bans(@CurrentUserId Long userId, @PathVariable String roomCode) {
        List<SpeakingBannedUserResponse> data = speakingRoomService.listBans(userId, roomCode);
        return ApiResponse.<List<SpeakingBannedUserResponse>>builder()
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
}
