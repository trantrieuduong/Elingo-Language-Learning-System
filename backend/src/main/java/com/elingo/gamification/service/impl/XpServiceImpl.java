package com.elingo.gamification.service.impl;

import com.elingo.common.event.LevelUpEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.XpEvent;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.service.BadgeAwardService;
import com.elingo.gamification.service.LeaderboardService;
import com.elingo.gamification.catalog.LevelCatalog;
import com.elingo.gamification.service.XpService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "XP-SERVICE")
public class XpServiceImpl implements XpService {

    private final UserXpRepository userXpRepository;
    private final XpEventRepository xpEventRepository;
    private final ApplicationEventPublisher publisher;
    private final UserRepository userRepository;
    private final LevelCatalog levelCatalog;
    private final LeaderboardService leaderboardService;
    private final BadgeAwardService badgeAwardService;

    @Override
    @Transactional
    public void addXp(long userId, int amount, XpSource source, Long refId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        userXpRepository.upsertAddXp(userId, amount); // tự tạo dòng lần đầu
        xpEventRepository.save(XpEvent.builder()
                .user(user)
                .source(source)
                .refId(refId)
                .amount(amount)
                .build());

        log.info("Xp added amount={} source={} refId={}", amount, source, refId);

        leaderboardService.recordXpForLeaderboard(userId, amount);
        badgeAwardService.evaluateAndAward(userId, BadgeConditionType.TOTAL_XP);
        evaluateSourceSpecificBadges(userId, source);

        int level = levelCatalog.levelFor(userXpRepository.findTotalXp(userId));
        if (userXpRepository.raiseLevel(userId, level) == 1) {
            log.info("Level upgraded newLevel={}", level);
            publisher.publishEvent(new LevelUpEvent(userId, level, LocalDateTime.now()));
        }
    }

    private void evaluateSourceSpecificBadges(long userId, XpSource source) {
        if (source == null) return;
        switch (source) {
            case BATTLE_WIN -> badgeAwardService.evaluateAndAward(userId, BadgeConditionType.BATTLE_WINS);
            case SEGMENT_DICTATION, SEGMENT_SHADOWING -> badgeAwardService
                    .evaluateAndAward(userId, BadgeConditionType.SEGMENT_COMPLETED);
            case FLASHCARD_REVIEW -> badgeAwardService.evaluateAndAward(userId, BadgeConditionType.WORDS_LEARNED);
            default -> { /* No specific badge condition for other sources */ }
        }
    }
}
