package com.elingo.battle.listener;

import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

// Lắng nghe STOMP disconnect để dọn hàng đợi và phòng riêng chưa có người vào
@Component
@RequiredArgsConstructor
public class BattleSessionDisconnectListener {

    private final BattleQueueService battleQueueService;
    private final BattleRoomService battleRoomService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) return;
        try {
            Long userId = Long.parseLong(user.getName());
            battleQueueService.removeOnDisconnect(userId);
            battleRoomService.onDisconnect(userId);
        } catch (NumberFormatException ignored) {    
        }
    }
}
