package com.elingo.battle.listener;

import com.elingo.battle.service.BattleQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

// Lắng nghe sự kiện STOMP disconnect để dọn hàng đợi
@Component
@RequiredArgsConstructor
public class BattleSessionDisconnectListener {

    private final BattleQueueService battleQueueService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) return;
        try {
            Long userId = Long.parseLong(user.getName());
            battleQueueService.removeOnDisconnect(userId);
        } catch (NumberFormatException ignored) {    
        }
    }
}
