package com.elingo.premium.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.premium.dto.projection.PlanSubscriptionStat;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.premium.service.SubscriptionService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "SUBSCRIPTION-SERVICE")
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserRepository userRepository;
    private final PremiumPlanRepository premiumPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @Override
    @Transactional
    public UserSubscription activateOrExtend(Long userId, Long planId) {
        // 1. Khóa hàng users của user để tránh race condition khi gia hạn song song
        User user = userRepository.findByIdForUpdate(userId) // Anchor Entity Locking
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        // 2. Lấy thông tin gói premium plan
        PremiumPlan plan = premiumPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(AppError.PREMIUM_PLAN_NOT_FOUND));

        int durationDays = plan.getDurationDays();
        LocalDateTime now = LocalDateTime.now();

        // 3. Lấy subscription ACTIVE mới nhất của user
        Optional<UserSubscription> latestActiveOpt = userSubscriptionRepository
                .findFirstByUserIdAndStatusOrderByEndAtDesc(userId, SubscriptionStatus.ACTIVE);

        if (latestActiveOpt.isPresent()) {
            UserSubscription sub = latestActiveOpt.get();
            // Nếu còn hạn: end_at += duration_days
            if (sub.getEndAt() != null && sub.getEndAt().isAfter(now)) {
                sub.setEndAt(sub.getEndAt().plusDays(durationDays));
                UserSubscription updated = userSubscriptionRepository.save(sub);
                log.info("Extended premium subscription id={} userId={} newEndAt={}",
                        updated.getId(), userId, updated.getEndAt());
                return updated;
            } else {
                // Đã hết hạn, đánh dấu EXPIRED bản ghi cũ
                sub.setStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(sub);
            }
        }

        // 4. Nếu chưa có hoặc đã hết hạn: tạo mới start_at = now, end_at = now + duration_days
        UserSubscription newSub = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startAt(now)
                .endAt(now.plusDays(durationDays))
                .build();

        UserSubscription created = userSubscriptionRepository.save(newSub);
        log.info("Created new premium subscription id={} userId={} planId={} startAt={} endAt={}",
                created.getId(), userId, planId, created.getStartAt(), created.getEndAt());
        return created;
    }

    @Override
    @Transactional
    public Duration revokeSubscriptionTime(Long userId, Long subscriptionId, int planDurationDays, LocalDateTime paidAt) {
        if (userRepository.findByIdForUpdate(userId).isEmpty()) {
            throw new AppException(AppError.USER_NOT_FOUND);
        }

        if (subscriptionId == null) {
            return Duration.ZERO;
        }

        UserSubscription sub = userSubscriptionRepository
                .findById(subscriptionId).orElse(null);
        if (sub == null || sub.getStatus() != SubscriptionStatus.ACTIVE) {
            return Duration.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        if (sub.getEndAt() == null || !sub.getEndAt().isAfter(now)) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            userSubscriptionRepository.save(sub);
            return Duration.ZERO;
        }

        LocalDateTime effectivePaidAt = paidAt != null ? paidAt : sub.getStartAt();
        Duration elapsed = Duration.between(effectivePaidAt, now);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }

        Duration revokeDuration = Duration.ofDays(planDurationDays).minus(elapsed);
        if (revokeDuration.isNegative() || revokeDuration.isZero()) {
            return Duration.ZERO;
        }

        LocalDateTime newEndAt = sub.getEndAt().minus(revokeDuration);
        if (!newEndAt.isAfter(now)) {
            sub.setEndAt(now);
            sub.setStatus(SubscriptionStatus.CANCELLED);
        } else {
            sub.setEndAt(newEndAt);
        }

        userSubscriptionRepository.save(sub);
        log.info("Revoked subscription time subId={} userId={} revokedHours={} newStatus={} newEndAt={}",
                sub.getId(), userId, revokeDuration.toHours(), sub.getStatus(), sub.getEndAt());
        return revokeDuration;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanSubscriptionStat> countActiveSubscriptionsByPlan(LocalDateTime now) {
        return userSubscriptionRepository.countActiveSubscriptionsByPlan(now);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDistinctActivePremiumUsers(LocalDateTime now) {
        return userSubscriptionRepository.countDistinctActivePremiumUsers(now);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTotalActiveSubscriptions(LocalDateTime now) {
        return userSubscriptionRepository.countTotalActiveSubscriptions(now);
    }
}

