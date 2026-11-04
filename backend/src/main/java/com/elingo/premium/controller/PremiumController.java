package com.elingo.premium.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.CreateCheckoutRequest;
import com.elingo.premium.dto.response.CheckoutResponse;
import com.elingo.premium.dto.response.MySubscriptionResponse;
import com.elingo.premium.dto.response.PaymentStatusResponse;
import com.elingo.premium.dto.response.PaymentTransactionResponse;
import com.elingo.premium.dto.response.PremiumPlanResponse;
import com.elingo.premium.service.PremiumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/premium")
@RequiredArgsConstructor
@Tag(name = "Premium Controller")
public class PremiumController {

    private final PremiumService premiumService;

    @GetMapping("/plans")
    @Operation(summary = "Get list of active premium plans")
    public ApiResponse<List<PremiumPlanResponse>> getActivePlans() {
        List<PremiumPlanResponse> plans = premiumService.getActivePlans();
        return ApiResponse.<List<PremiumPlanResponse>>builder()
                .success(true)
                .data(plans)
                .build();
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create a checkout request for a premium plan and retrieve payment QR transfer details")
    public ApiResponse<CheckoutResponse> checkout(
            @CurrentUserId Long userId,
            @Valid @RequestBody CreateCheckoutRequest request
    ) {
        CheckoutResponse response = premiumService.checkout(userId, request);
        return ApiResponse.<CheckoutResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/transactions/{transactionId}")
    @Operation(summary = "Get payment transaction status for client polling")
    public ApiResponse<PaymentStatusResponse> getTransactionStatus(
            @CurrentUserId Long userId,
            @PathVariable Long transactionId
    ) {
        PaymentStatusResponse response = premiumService.getTransactionStatus(userId, transactionId);
        return ApiResponse.<PaymentStatusResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Get premium plan information and usage period for the logged-in user")
    public ApiResponse<MySubscriptionResponse> getMySubscription(
            @CurrentUserId Long userId
    ) {
        MySubscriptionResponse response = premiumService.getMySubscription(userId);
        return ApiResponse.<MySubscriptionResponse>builder()
                .success(true)
                .data(response)
                .build();
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get payment transaction history for the logged-in user")
    public ApiResponse<PageResponse<PaymentTransactionResponse>> getMyTransactions(
            @CurrentUserId Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PaymentTransactionResponse> response = premiumService.getMyTransactions(userId, pageable);
        return ApiResponse.<PageResponse<PaymentTransactionResponse>>builder()
                .success(true)
                .data(response)
                .build();
    }
}
