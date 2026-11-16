package com.elingo.speaking.listener;

import com.elingo.speaking.service.SpeakingConnectionService;
import com.elingo.speaking.state.SpeakingSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

// Lắng nghe STOMP disconnect và xử lý trạng thái offline
@Slf4j(topic = "SPEAKING-SESSION-DISCONNECT-LISTENER")
@Component
@RequiredArgsConstructor
public class SpeakingSessionDisconnectListener {

    private final SpeakingSessionRegistry sessionRegistry;
    private final SpeakingConnectionService connectionService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        if (user == null) return;
        try {
            Long userId = Long.parseLong(user.getName());
            String sessionId = event.getSessionId();
            if (!sessionRegistry.removeAndCheckLast(userId, sessionId)) return;
            log.info("Speaking disconnect: last session closed userId={} sessionId={}", userId, sessionId);
            connectionService.onUserOffline(userId);
        } catch (NumberFormatException ignored) {
            log.warn("Speaking disconnect: principal name is not a valid userId: {}", user.getName());
        }
    }
}
