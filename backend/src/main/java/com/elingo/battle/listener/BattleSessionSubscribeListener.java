package com.elingo.battle.listener;

import com.elingo.battle.service.BattleConnectionService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import com.elingo.battle.state.BattleSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

// Lắng nghe STOMP subscribe/unsubscribe để quản lý session, dọn hàng đợi, phòng chưa có người vào, và báo mất kết nối giữa trận khi session cuối đóng
@Slf4j(topic = "BATTLE-SESSION-SUBSCRIBE-LISTENER")
@Component
@RequiredArgsConstructor
public class BattleSessionSubscribeListener {

    private final BattleSessionRegistry sessionRegistry;
    private final BattleQueueService battleQueueService;
    private final BattleRoomService battleRoomService;
    private final BattleConnectionService battleConnectionService;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) return;

        if (!isBattleQueueDestination(destination)) {
            return;
        }

        Principal user = accessor.getUser();
        if (user == null && event.getMessage() != null) {
            Object userHeader = event.getMessage().getHeaders().get(SimpMessageHeaderAccessor.USER_HEADER);
            if (userHeader instanceof Principal p) {
                user = p;
            }
        }
        if (user == null) return;

        try {
            Long userId = Long.parseLong(user.getName());
            String sessionId = accessor.getSessionId();
            String subscriptionId = accessor.getSubscriptionId();
            sessionRegistry.addSubscription(userId, sessionId, subscriptionId);
            log.debug("Battle subscription added userId={} sessionId={} subId={} dest={}",
                    userId, sessionId, subscriptionId, destination);
        } catch (NumberFormatException ignored) {
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String subscriptionId = accessor.getSubscriptionId();

        Long offlineUserId = sessionRegistry.removeSubscription(sessionId, subscriptionId);
        if (offlineUserId != null) {
            log.info("Battle subscription removed & last session closed userId={}", offlineUserId);
            battleQueueService.removeOnDisconnect(offlineUserId);
            battleRoomService.onDisconnect(offlineUserId);
            battleConnectionService.onUserOffline(offlineUserId);
        }
    }

    private boolean isBattleQueueDestination(String destination) {
        return "/user/queue/battle".equals(destination)
                || "/queue/battle".equals(destination)
                || destination.endsWith("/queue/battle");
    }
}

