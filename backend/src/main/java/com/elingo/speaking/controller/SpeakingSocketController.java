package com.elingo.speaking.controller;

import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.JoinSpeakingRoomRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.service.SpeakingRoomService;
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

    private final SpeakingRoomService speakingRoomService;
    private final SpeakingSignalingService signalingService;

    @MessageMapping("/speaking/create")
    public void create(@Valid @Payload CreateSpeakingRoomRequest request, Principal principal) {
        speakingRoomService.createRoom(Long.parseLong(principal.getName()), request);
    }

    @MessageMapping("/speaking/join")
    public void join(@Valid @Payload JoinSpeakingRoomRequest request, Principal principal) {
        speakingRoomService.joinRoom(Long.parseLong(principal.getName()), request.roomCode());
    }

    @MessageMapping("/speaking/leave")
    public void leave(Principal principal) {
        speakingRoomService.leaveRoom(Long.parseLong(principal.getName()));
    }

    @MessageMapping("/speaking/signal")
    public void signal(@Valid @Payload SpeakingSignalRequest request, Principal principal) {
        signalingService.relaySignal(Long.parseLong(principal.getName()), request);
    }
}
