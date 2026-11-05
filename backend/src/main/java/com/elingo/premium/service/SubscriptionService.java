package com.elingo.premium.service;

import com.elingo.premium.entity.UserSubscription;

public interface SubscriptionService {
    UserSubscription activateOrExtend(Long userId, Long planId);
}
