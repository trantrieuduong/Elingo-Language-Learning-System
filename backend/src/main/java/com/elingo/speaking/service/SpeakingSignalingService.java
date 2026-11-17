package com.elingo.speaking.service;

import com.elingo.speaking.dto.request.SpeakingChatRequest;
import com.elingo.speaking.dto.request.SpeakingMicRequest;
import com.elingo.speaking.dto.request.SpeakingMuteRequest;
import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;

public interface SpeakingSignalingService {
    void goOnline(Long userId, GoOnlineContext ctx);
    void relaySignal(Long fromUserId, SpeakingSignalRequest request);
    void reportMic(Long userId, SpeakingMicRequest req);
    void requestMute(Long hostId, SpeakingMuteRequest req);
    void chat(Long userId, SpeakingChatRequest req);
}
