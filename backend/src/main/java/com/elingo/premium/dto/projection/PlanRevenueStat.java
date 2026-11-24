package com.elingo.premium.dto.projection;

import java.math.BigDecimal;

public interface PlanRevenueStat {
    Long getPlanId();
    String getPlanName();
    BigDecimal getTotalAmount();
    Long getTxCount();
}
