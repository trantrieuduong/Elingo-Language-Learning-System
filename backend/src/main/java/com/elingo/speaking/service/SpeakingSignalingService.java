package com.elingo.speaking.service;

import com.elingo.speaking.dto.request.SpeakingSignalRequest;
import com.elingo.speaking.service.SpeakingRoomPersistenceService.GoOnlineContext;

public interface SpeakingSignalingService {
    void goOnline(Long userId, GoOnlineContext ctx);
    void relaySignal(Long fromUserId, SpeakingSignalRequest request);
}
