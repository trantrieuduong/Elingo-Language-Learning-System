package com.elingo.gamification.listener;

import com.elingo.common.event.XpGainedEvent;
import com.elingo.gamification.config.GamificationProperties;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.service.CheckInService;
import com.elingo.gamification.service.XpPolicy;
import com.elingo.gamification.service.XpService;
import com.elingo.gamification.websocket.GamificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "LEARNING-XP-LISTENER")
public class LearningXpListener {

    private final CheckInService checkInService;
    private final XpService xpService;
    private final XpPolicy xpPolicy;
    private final GamificationProperties props;
    private final GamificationEventPublisher gamificationEventPublisher;

    @Async("gamificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(XpGainedEvent event) {
        var rule = props.rule(event.xpSource());

        if (rule == null || event.xpSource() == XpSource.DAILY_CHECKIN) return;

        try {
            checkInService.ensureCheckedIn(event.userId(), event.occurredAt());

            if (xpPolicy.allow(event.userId(), event.xpSource(), event.occurredAt().toLocalDate())) {
                xpService.addXp(event.userId(), rule.xp(), event.xpSource(), event.refId());
                gamificationEventPublisher.sendXpGained(event.userId(), rule.xp());
            }
        } catch (Exception ex) {
            log.error("XP failed userId={} source={} refId={}", event.userId(), event.xpSource(), event.refId(), ex);
        }
    }
}
