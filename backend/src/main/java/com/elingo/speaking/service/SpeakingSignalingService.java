package com.elingo.speaking.service;

import com.elingo.speaking.dto.request.SpeakingSignalRequest;

public interface SpeakingSignalingService {
    void enter(Long userId, String roomCode);
    void relaySignal(Long fromUserId, SpeakingSignalRequest request);
}
