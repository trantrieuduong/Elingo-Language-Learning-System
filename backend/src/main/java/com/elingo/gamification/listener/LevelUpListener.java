package com.elingo.gamification.listener;

import com.elingo.common.event.LevelUpEvent;
import com.elingo.gamification.catalog.LevelCatalog;
import com.elingo.gamification.dto.response.LevelUpWsResponse;
import com.elingo.gamification.publisher.GamificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "LEVEL-UP-LISTENER")
public class LevelUpListener {

    private final LevelCatalog levelCatalog;
    private final GamificationEventPublisher gamificationEventPublisher;

    @Async("gamificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(LevelUpEvent event) {
        log.info("Level up event received userId={} newLevel={}", event.userId(), event.newLevel());
        try {
            String title = levelCatalog.getTitleForLevel(event.newLevel());
            LevelUpWsResponse payload = new LevelUpWsResponse(
                    event.newLevel(),
                    title,
                    event.occurredAt()
            );

            gamificationEventPublisher.sendLevelUp(event.userId(), payload);
            log.info("Level up notification sent userId={} newLevel={} title={}",
                    event.userId(), event.newLevel(), title);
        } catch (Exception ex) {
            log.error("Failed to process level up notification userId={} newLevel={}",
                    event.userId(), event.newLevel(), ex);
        }
    }
}
