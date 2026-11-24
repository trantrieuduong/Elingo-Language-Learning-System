package com.elingo.premium.dto.projection;

import com.elingo.premium.entity.PaymentTransactionStatus;

public interface TransactionStatusCount {
    PaymentTransactionStatus getStatus();
    Long getCountVal();
}
