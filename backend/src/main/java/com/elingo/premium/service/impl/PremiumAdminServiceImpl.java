package com.elingo.premium.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.NotificationEvent;
import com.elingo.common.event.PaymentSucceededEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.PaymentWebhookLog;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.mapper.PremiumMapper;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.specification.PaymentTransactionSpecifications;
import com.elingo.premium.repository.PaymentWebhookLogRepository;
import com.elingo.premium.repository.specification.PaymentWebhookLogSpecifications;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.service.PremiumAdminService;
import com.elingo.premium.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PREMIUM-ADMIN-SERVICE")
public class PremiumAdminServiceImpl implements PremiumAdminService {

    private static final DateTimeFormatter EXPIRES_AT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PremiumPlanRepository premiumPlanRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentWebhookLogRepository paymentWebhookLogRepository;
    private final SubscriptionService subscriptionService;
    private final PremiumMapper premiumMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminPremiumPlanResponse> getAllPlansForAdmin() {
        return premiumMapper.toAdminPremiumPlanResponseList(
                premiumPlanRepository.findAllByOrderByPriceAscIdAsc()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPremiumPlanResponse getPlanByIdForAdmin(Long planId) {
        PremiumPlan plan = findPlanOrThrow(planId);
        return premiumMapper.toAdminPremiumPlanResponse(plan);
    }

    @Override
    @Transactional
    public AdminPremiumPlanResponse createPlan(CreatePremiumPlanRequest request) {
        PremiumPlan plan = premiumMapper.toEntity(request);
        plan.setCurrency(request.currency().trim().toUpperCase());
        plan.setIsActive(false);
        PremiumPlan saved = premiumPlanRepository.save(plan);
        return premiumMapper.toAdminPremiumPlanResponse(saved);
    }

    @Override
    @Transactional
    public AdminPremiumPlanResponse updatePlan(Long planId, UpdatePremiumPlanRequest request) {
        PremiumPlan plan = findPlanOrThrow(planId);
        premiumMapper.updateEntity(plan, request);
        PremiumPlan saved = premiumPlanRepository.save(plan);
        return premiumMapper.toAdminPremiumPlanResponse(saved);
    }

    @Override
    @Transactional
    public AdminPremiumPlanResponse disablePlan(Long planId) {
        PremiumPlan plan = findPlanOrThrow(planId);
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new AppException(AppError.PREMIUM_PLAN_ALREADY_INACTIVE);
        }
        plan.setIsActive(false);
        return premiumMapper.toAdminPremiumPlanResponse(premiumPlanRepository.save(plan));
    }

    @Override
    @Transactional
    public AdminPremiumPlanResponse enablePlan(Long planId) {
        PremiumPlan plan = findPlanOrThrow(planId);
        if (Boolean.TRUE.equals(plan.getIsActive())) {
            throw new AppException(AppError.PREMIUM_PLAN_ALREADY_ACTIVE);
        }
        plan.setIsActive(true);
        return premiumMapper.toAdminPremiumPlanResponse(premiumPlanRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminTransactionResponse> searchTransactionsForAdmin(
            AdminTransactionFilterRequest filter,
            Pageable pageable
    ) {
        Page<PaymentTransaction> page = paymentTransactionRepository.findAll(
                PaymentTransactionSpecifications.withFilter(filter),
                pageable
        );
        return PageResponse.of(page.map(premiumMapper::toAdminTransactionResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTransactionDetailResponse getTransactionByIdForAdmin(Long transactionId) {
        return loadTransactionDetail(transactionId);
    }

    @Override
    @Transactional
    public AdminTransactionDetailResponse confirmTransactionManually(
            Long adminId,
            Long transactionId,
            TransactionReasonRequest request
    ) {
        PaymentTransaction tx = paymentTransactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(AppError.PAYMENT_TRANSACTION_NOT_FOUND));

        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            throw new AppException(AppError.PAYMENT_TRANSACTION_ALREADY_PROCESSED);
        }

        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        tx.setPaidAt(now);

        Map<String, Object> extraMeta = new LinkedHashMap<>();
        extraMeta.put("manualConfirmed", true);
        extraMeta.put("confirmedByAdminId", adminId);
        extraMeta.put("confirmReason", request.reason());
        extraMeta.put("confirmedAt", now.toString());
        tx.setMetadata(mergeMetadata(tx.getMetadata(), extraMeta));

        UserSubscription subscription = subscriptionService.activateOrExtend(
                tx.getUser().getId(),
                tx.getPlan().getId()
        );
        tx.setSubscription(subscription);
        paymentTransactionRepository.save(tx);

        eventPublisher.publishEvent(new PaymentSucceededEvent(
                tx.getId(),
                tx.getUser().getId(),
                tx.getPlan().getId(),
                tx.getAmount(),
                tx.getPaidAt()
        ));

        String expiresAt = subscription.getEndAt() != null
                ? subscription.getEndAt().format(EXPIRES_AT_FORMATTER)
                : "";
        eventPublisher.publishEvent(NotificationEvent.subscriptionActivated(
                tx.getUser().getId(),
                subscription.getId(),
                tx.getPlan().getName(),
                expiresAt
        ));

        return loadTransactionDetail(tx.getId());
    }

    @Override
    @Transactional
    public AdminTransactionDetailResponse refundTransaction(
            Long adminId,
            Long transactionId,
            TransactionReasonRequest request
    ) {
        PaymentTransaction tx = paymentTransactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(AppError.PAYMENT_TRANSACTION_NOT_FOUND));

        if (tx.getStatus() != PaymentTransactionStatus.SUCCESS) {
            throw new AppException(AppError.PAYMENT_TRANSACTION_NOT_REFUNDABLE);
        }

        Long subscriptionId = tx.getSubscription() != null ? tx.getSubscription().getId() : null;
        Duration revoked = subscriptionService.revokeSubscriptionTime(
                tx.getUser().getId(),
                subscriptionId,
                tx.getPlan().getDurationDays(),
                tx.getPaidAt()
        );

        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(PaymentTransactionStatus.REFUNDED);

        Map<String, Object> extraMeta = new LinkedHashMap<>();
        extraMeta.put("refunded", true);
        extraMeta.put("refundedByAdminId", adminId);
        extraMeta.put("refundReason", request.reason());
        extraMeta.put("refundedAt", now.toString());
        extraMeta.put("revokedMinutes", revoked.toMinutes());
        tx.setMetadata(mergeMetadata(tx.getMetadata(), extraMeta));

        paymentTransactionRepository.save(tx);
        return loadTransactionDetail(tx.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminWebhookLogResponse> searchWebhookLogsForAdmin(
            AdminWebhookLogFilterRequest filter,
            Pageable pageable
    ) {
        Page<PaymentWebhookLog> page = paymentWebhookLogRepository.findAll(
                PaymentWebhookLogSpecifications.withFilter(filter),
                pageable
        );
        return PageResponse.of(page.map(premiumMapper::toAdminWebhookLogResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminWebhookLogDetailResponse getWebhookLogByIdForAdmin(Long logId) {
        PaymentWebhookLog webhookLog = paymentWebhookLogRepository.findDetailById(logId)
                .orElseThrow(() -> new AppException(AppError.PAYMENT_WEBHOOK_LOG_NOT_FOUND));
        return premiumMapper.toAdminWebhookLogDetailResponse(webhookLog);
    }

    private PremiumPlan findPlanOrThrow(Long planId) {
        return premiumPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(AppError.PREMIUM_PLAN_NOT_FOUND));
    }

    private AdminTransactionDetailResponse loadTransactionDetail(Long transactionId) {
        PaymentTransaction tx = paymentTransactionRepository.findDetailById(transactionId)
                .orElseThrow(() -> new AppException(AppError.PAYMENT_TRANSACTION_NOT_FOUND));
        return premiumMapper.toAdminTransactionDetailResponse(tx);
    }

    private String mergeMetadata(String existingJson, Map<String, Object> additions) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existingJson != null && !existingJson.isBlank()) {
            try {
                Map<String, Object> current = objectMapper.readValue(
                        existingJson,
                        new TypeReference<Map<String, Object>>() {}
                );
                if (current != null) {
                    merged.putAll(current);
                }
            } catch (Exception ex) {
                log.warn("Failed to parse existing transaction metadata, preserving raw value");
                merged.put("previousRawMetadata", existingJson);
            }
        }
        merged.putAll(additions);
        try {
            return objectMapper.writeValueAsString(merged);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize transaction metadata", ex);
        }
    }
}
