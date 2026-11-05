package com.elingo.premium.service.impl;

import com.elingo.common.exception.AppError;
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

import java.time.LocalDateTime;
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
}

