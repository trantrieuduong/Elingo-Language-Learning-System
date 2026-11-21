package com.elingo.speaking.controller;

import com.elingo.speaking.dto.request.CreateSpeakingRoomRequest;
import com.elingo.speaking.dto.request.EndSpeakingRoomRequest;
import com.elingo.speaking.dto.request.JoinSpeakingRoomRequest;
import com.elingo.speaking.dto.request.KickMemberRequest;
import com.elingo.speaking.dto.request.SpeakingChatRequest;
import com.elingo.speaking.dto.request.SpeakingMicRequest;
import com.elingo.speaking.dto.request.SpeakingMuteRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.dto.request.TransferHostRequest;
import com.elingo.speaking.dto.request.UnbanMemberRequest;
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

    @MessageMapping("/speaking/transfer-host")
    public void transferHost(@Valid @Payload TransferHostRequest request, Principal principal) {
        speakingRoomService.transferHost(Long.parseLong(principal.getName()), request.roomCode(), request.newHostId());
    }

    @MessageMapping("/speaking/end")
    public void end(@Valid @Payload EndSpeakingRoomRequest request, Principal principal) {
        speakingRoomService.endRoom(Long.parseLong(principal.getName()), request.roomCode());
    }

    @MessageMapping("/speaking/signal")
    public void signal(@Valid @Payload SpeakingSignalRequest request, Principal principal) {
        signalingService.relaySignal(Long.parseLong(principal.getName()), request);
    }

    @MessageMapping("/speaking/mic")
    public void mic(@Valid @Payload SpeakingMicRequest request, Principal principal) {
        signalingService.reportMic(Long.parseLong(principal.getName()), request);
    }

    @MessageMapping("/speaking/mute")
    public void mute(@Valid @Payload SpeakingMuteRequest request, Principal principal) {
        signalingService.requestMute(Long.parseLong(principal.getName()), request);
    }

    @MessageMapping("/speaking/kick")
    public void kick(@Valid @Payload KickMemberRequest request, Principal principal) {
        speakingRoomService.kickMember(Long.parseLong(principal.getName()), request.roomCode(), request.userId());
    }

    @MessageMapping("/speaking/unban")
    public void unban(@Valid @Payload UnbanMemberRequest request, Principal principal) {
        speakingRoomService.unbanMember(Long.parseLong(principal.getName()), request.roomCode(), request.userId());
    }

    @MessageMapping("/speaking/chat")
    public void chat(@Valid @Payload SpeakingChatRequest request, Principal principal) {
        signalingService.chat(Long.parseLong(principal.getName()), request);
    }

    @MessageMapping("/speaking/active-room")
    public void activeRoom(Principal principal) {
        try {
            speakingRoomService.sendActiveRoom(Long.parseLong(principal.getName()));
        } catch (Exception ex) {
            // không trả error, bỏ qua / log
        }
    }
}
