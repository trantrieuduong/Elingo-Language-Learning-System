package com.elingo.premium.dto.projection;

public interface PlanSubscriptionStat {
    Long getPlanId();
    String getPlanName();
    Long getActiveCount();
}
