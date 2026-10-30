package com.elingo.battle.controller;

import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.request.QueueJoinRequest;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.service.BattleQueueService;
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
}
