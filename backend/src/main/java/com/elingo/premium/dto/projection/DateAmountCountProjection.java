package com.elingo.premium.dto.projection;

import java.math.BigDecimal;

public interface DateAmountCountProjection {
    String getPeriodKey();
    BigDecimal getTotalAmount();
    Long getTxCount();
}
