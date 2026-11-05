package com.elingo.premium.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.CreateCheckoutRequest;
import com.elingo.premium.dto.response.CheckoutResponse;
import com.elingo.premium.dto.response.MySubscriptionResponse;
import com.elingo.premium.dto.response.PaymentStatusResponse;
import com.elingo.premium.dto.response.PaymentTransactionResponse;
import com.elingo.premium.dto.response.PremiumPlanResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PremiumService {
    List<PremiumPlanResponse> getActivePlans();
    CheckoutResponse checkout(Long userId, CreateCheckoutRequest request);
    PaymentStatusResponse getTransactionStatus(Long userId, Long transactionId);
    MySubscriptionResponse getMySubscription(Long userId);
    PageResponse<PaymentTransactionResponse> getMyTransactions(Long userId, Pageable pageable);
}
