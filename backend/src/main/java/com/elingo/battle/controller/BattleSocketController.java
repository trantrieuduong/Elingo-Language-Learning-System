package com.elingo.battle.controller;

import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.request.QueueJoinRequest;
import com.elingo.battle.dto.request.RejoinRequest;
import com.elingo.battle.dto.request.RoomCreateRequest;
import com.elingo.battle.dto.request.RoomJoinRequest;
import com.elingo.battle.service.BattleConnectionService;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class BattleSocketController {

    private final BattleQueueService battleQueueService;
    private final BattleEngineService battleEngineService;
    private final BattleRoomService battleRoomService;
    private final BattleConnectionService battleConnectionService;

    @MessageMapping("/battle/queue/join")
    public void joinQueue(@Valid @Payload QueueJoinRequest request, Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        battleQueueService.join(userId, request.mode());
    }

    @MessageMapping("/battle/queue/leave")
    public void leaveQueue(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        battleQueueService.leave(userId);
    }

    @MessageMapping("/battle/answer")
    public void answer(@Valid @Payload BattleAnswerRequest request, Principal principal) {
        battleEngineService.submitAnswer(Long.parseLong(principal.getName()), request);
    }

    @MessageMapping("/battle/room/create")
    public void createRoom(@Valid @Payload RoomCreateRequest request, Principal principal) {
        battleRoomService.createRoom(Long.parseLong(principal.getName()), request.mode());
    }

    @MessageMapping("/battle/room/join")
    public void joinRoom(@Valid @Payload RoomJoinRequest request, Principal principal) {
        battleRoomService.joinRoom(Long.parseLong(principal.getName()), request.roomCode());
    }

    @MessageMapping("/battle/room/leave")
    public void leaveRoom(Principal principal) {
        battleRoomService.leaveRoom(Long.parseLong(principal.getName()));
    }

    @MessageMapping("/battle/rejoin")
    public void rejoin(@Valid @Payload RejoinRequest request, Principal principal) {
        battleConnectionService.rejoin(Long.parseLong(principal.getName()), request.matchId());
    }

    @MessageMapping("/battle/active-match")
    public void activeMatch(Principal principal) {
        battleConnectionService.sendActiveMatch(Long.parseLong(principal.getName()));
    }
}
