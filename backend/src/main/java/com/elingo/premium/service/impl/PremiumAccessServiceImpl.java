package com.elingo.premium.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.entity.SubscriptionStatus;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.premium.service.PremiumAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PremiumAccessServiceImpl implements PremiumAccessService {

    private final UserSubscriptionRepository subscriptionRepository;

    @Override
    public boolean hasActivePremium(Long userId) {
        return userId != null
                && subscriptionRepository.existsByUserIdAndStatusAndEndAtAfter(
                userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
    }

    @Override
    public void requirePremium(Long userId) {
        if (!hasActivePremium(userId))
            throw new AppException(AppError.PREMIUM_REQUIRED);
    }
}
