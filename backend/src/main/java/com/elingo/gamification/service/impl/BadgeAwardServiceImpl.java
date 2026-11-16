package com.elingo.gamification.service.impl;

import com.elingo.common.event.NotificationEvent;
import com.elingo.gamification.entity.Badge;
import com.elingo.gamification.entity.BadgeConditionType;
import com.elingo.gamification.entity.UserBadge;
import com.elingo.gamification.repository.UserBadgeRepository;
import com.elingo.gamification.service.BadgeAwardService;
import com.elingo.gamification.catalog.BadgeCatalog;
import com.elingo.gamification.service.BadgeEvaluator;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "BADGE-AWARD-SERVICE")
public class BadgeAwardServiceImpl implements BadgeAwardService {

    private final List<BadgeEvaluator> evaluators;
    private final BadgeCatalog badgeCatalog;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final Map<BadgeConditionType, BadgeEvaluator> evaluatorMap = new EnumMap<>(BadgeConditionType.class);

    @PostConstruct
    public void init() {
        for (BadgeEvaluator evaluator : evaluators) {
            evaluatorMap.put(evaluator.supportsType(), evaluator);
        }
    }

    @Override
    @Transactional
    public List<Badge> evaluateAndAward(Long userId, BadgeConditionType conditionType) {
        if (userId == null)
            return List.of();

        BadgeEvaluator evaluator = evaluatorMap.get(conditionType);
        if (evaluator == null)
            return List.of();

        List<Badge> candidateBadges = badgeCatalog.getBadgesByType(conditionType);
        if (candidateBadges.isEmpty())
            return List.of();

        Set<Long> earnedBadgeIds = userBadgeRepository.findEarnedBadgeIdsByUserId(userId);

        List<Badge> unearnedBadges = candidateBadges.stream()
                .filter(badge -> !earnedBadgeIds.contains(badge.getId()))
                .toList();

        if (unearnedBadges.isEmpty())
            return List.of();

        long currentProgress = evaluator.getCurrentProgress(userId);

        List<Badge> awardedBadges = unearnedBadges.stream()
                .filter(badge -> evaluator.isEligible(currentProgress, badge))
                .toList();

        if (awardedBadges.isEmpty())
            return List.of();

        User user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();

        List<UserBadge> newUserBadges = new ArrayList<>(awardedBadges.size());
        for (Badge badge : awardedBadges) {
            UserBadge ub = UserBadge.builder()
                    .user(user)
                    .badge(badge)
                    .earnedAt(now)
                    .build();
            newUserBadges.add(ub);
        }

        userBadgeRepository.saveAll(newUserBadges);

        for (Badge badge : awardedBadges) {
            applicationEventPublisher.publishEvent(NotificationEvent.badgeEarned(
                    userId,
                    badge.getId(),
                    badge.getName()
            ));
            log.info("Badge awarded userId={} badgeCode={} conditionType={} progress={}",
                    userId, badge.getCode(), conditionType, currentProgress);
        }

        return awardedBadges;
    }
}
