package com.elingo.battle.listener;

import com.elingo.battle.service.BattleConnectionService;
import com.elingo.battle.service.BattleQueueService;
import com.elingo.battle.service.BattleRoomService;
import com.elingo.battle.state.BattleSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

// Lắng nghe STOMP disconnect để dọn hàng đợi, phòng chưa có người vào, và báo mất kết nối giữa trận khi session cuối đóng
@Component
@RequiredArgsConstructor
public class BattleSessionDisconnectListener {

    private final BattleQueueService battleQueueService;
    private final BattleRoomService battleRoomService;
    private final BattleSessionRegistry sessionRegistry;
    private final BattleConnectionService battleConnectionService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        if (user == null) return;
        try {
            Long userId = Long.parseLong(user.getName());
            if (!sessionRegistry.removeAndCheckLast(userId, event.getSessionId())) return;

            battleQueueService.removeOnDisconnect(userId);
            battleRoomService.onDisconnect(userId);
            battleConnectionService.onUserOffline(userId);
        } catch (NumberFormatException ignored) {
        }
    }
}
