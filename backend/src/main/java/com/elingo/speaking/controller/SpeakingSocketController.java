package com.elingo.speaking.controller;

import com.elingo.speaking.dto.request.EnterRoomRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.service.SpeakingSignalingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class SpeakingSocketController {

    private final SpeakingSignalingService signalingService;

    @MessageMapping("/speaking/enter")
    public void enter(@Valid @Payload EnterRoomRequest request, Principal principal) {
        signalingService.enter(Long.parseLong(principal.getName()), request.roomCode());
    }

    @MessageMapping("/speaking/signal")
    public void signal(@Valid @Payload SpeakingSignalRequest request, Principal principal) {
        signalingService.relaySignal(Long.parseLong(principal.getName()), request);
    }
}
