package com.elingo.speaking.listener;

import com.elingo.speaking.service.SpeakingConnectionService;
import com.elingo.speaking.state.SpeakingSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

// Lắng nghe STOMP subscribe/unsubscribe để quản lý session và báo mất kết nối khi session cuối đóng
@Slf4j(topic = "SPEAKING-SESSION-SUBSCRIBE-LISTENER")
@Component
@RequiredArgsConstructor
public class SpeakingSessionSubscribeListener {

    private final SpeakingSessionRegistry sessionRegistry;
    private final SpeakingConnectionService connectionService;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !isSpeakingQueueDestination(destination)) return;

        Principal user = accessor.getUser();
        if (user == null && event.getMessage() != null) {
            Object h = event.getMessage().getHeaders().get(SimpMessageHeaderAccessor.USER_HEADER);
            if (h instanceof Principal p) user = p;
        }
        if (user == null) return;

        try {
            Long userId = Long.parseLong(user.getName());
            String sessionId = accessor.getSessionId();
            String subscriptionId = accessor.getSubscriptionId();
            sessionRegistry.addSubscription(userId, sessionId, subscriptionId);
            log.debug("Speaking subscription added userId={} sessionId={} subId={} dest={}",
                    userId, sessionId, subscriptionId, destination);
        } catch (NumberFormatException ignored) {
            log.warn("Speaking subscribe: principal name is not a valid userId: {}", user.getName());
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();
        Long offlineUserId = sessionRegistry.removeSubscription(sessionId, subscriptionId);
        if (offlineUserId != null) {
            log.info("Speaking subscription removed & last sub closed userId={}", offlineUserId);
            connectionService.onUserOffline(offlineUserId);
        }
    }

    private boolean isSpeakingQueueDestination(String destination) {
        return "/user/queue/speaking".equals(destination)
                || "/queue/speaking".equals(destination)
                || destination.endsWith("/queue/speaking");
    }
}
