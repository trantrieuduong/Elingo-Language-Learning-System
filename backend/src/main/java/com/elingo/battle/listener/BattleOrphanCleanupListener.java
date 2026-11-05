package com.elingo.battle.listener;

import com.elingo.battle.service.BattleMatchPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Dọn các trận Battle còn sót lại từ lần chạy trước
@Slf4j(topic = "BATTLE-ORPHAN-CLEANUP")
@Component
@RequiredArgsConstructor
public class BattleOrphanCleanupListener {

    private final BattleMatchPersistenceService persistence;

    @EventListener(ApplicationReadyEvent.class)
    public void cancelOrphans() {
        try {
            int n = persistence.cancelOrphanMatches();
            if (n > 0) {
                log.info("Cancelled {} orphan battle matches left from previous run", n);
            }
        } catch (Exception ex) {
            log.warn("Orphan battle cleanup failed: {}", ex.getMessage());
        }
    }
}

