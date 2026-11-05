package com.elingo.premium.service.impl;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.config.PremiumProperties;
import com.elingo.premium.dto.PaymentQrInfo;
import com.elingo.premium.dto.request.CreateCheckoutRequest;
import com.elingo.premium.dto.response.CheckoutResponse;
import com.elingo.premium.dto.response.MySubscriptionResponse;
import com.elingo.premium.dto.response.PaymentStatusResponse;
import com.elingo.premium.dto.response.PaymentTransactionResponse;
import com.elingo.premium.dto.response.PremiumPlanResponse;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.premium.entity.UserSubscription;
import com.elingo.premium.mapper.PremiumMapper;
import com.elingo.premium.repository.PaymentTransactionRepository;
import com.elingo.premium.repository.PremiumPlanRepository;
import com.elingo.premium.repository.UserSubscriptionRepository;
import com.elingo.premium.service.PaymentQrService;
import com.elingo.premium.service.PremiumService;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PREMIUM-SERVICE")
public class PremiumServiceImpl implements PremiumService {

    private final PremiumPlanRepository premiumPlanRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final PremiumMapper premiumMapper;
    private final PaymentQrService paymentQrService;
    private final PremiumProperties premiumProperties;

    @Override
    @Transactional(readOnly = true)
    public List<PremiumPlanResponse> getActivePlans() {
        List<PremiumPlan> plans = premiumPlanRepository.findAllByIsActiveTrueOrderByPriceAsc();
        return premiumMapper.toPremiumPlanResponseList(plans);
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, CreateCheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        PremiumPlan plan = premiumPlanRepository.findById(request.planId())
                .orElseThrow(() -> new AppException(AppError.PREMIUM_PLAN_NOT_FOUND));

        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new AppException(AppError.PREMIUM_PLAN_INACTIVE);
        }

        String prefix = premiumProperties.getBankTransfer().getCodePrefix();
        String refCode = generateUniqueRefCode(prefix);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .plan(plan)
                .provider(null)
                .amount(plan.getPrice())
                .currency(plan.getCurrency() != null ? plan.getCurrency() : "VND")
                .providerRefCode(refCode)
                .status(PaymentTransactionStatus.PENDING)
                .build();

        transaction = paymentTransactionRepository.save(transaction);
        log.info("Created checkout transaction id={} refCode={} userId={} planId={} amount={}",
                transaction.getId(), refCode, userId, plan.getId(), plan.getPrice());

        PaymentQrInfo qrInfo = paymentQrService.generateQrInfo(
                transaction.getAmount(),
                transaction.getCurrency(),
                refCode
        );

        return new CheckoutResponse(
                transaction.getId(),
                refCode,
                transaction.getAmount(),
                transaction.getCurrency(),
                qrInfo.bankName(),
                qrInfo.bankAccount(),
                qrInfo.bankAccountName(),
                qrInfo.qrCodeUrl(),
                transaction.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getTransactionStatus(Long userId, Long transactionId) {
        PaymentTransaction tx = paymentTransactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new AppException(AppError.PAYMENT_TRANSACTION_NOT_FOUND));

        return new PaymentStatusResponse(
                tx.getId(),
                tx.getProviderRefCode(),
                tx.getStatus(),
                tx.getStatus() == PaymentTransactionStatus.SUCCESS,
                tx.getPaidAt(),
                tx.getPlan() != null ? tx.getPlan().getName() : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MySubscriptionResponse getMySubscription(Long userId) {
        Optional<UserSubscription> subOpt = userSubscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);

        if (subOpt.isEmpty()) {
            return new MySubscriptionResponse(null, null, null, null, 0L);
        }

        UserSubscription sub = subOpt.get();
        LocalDateTime now = LocalDateTime.now();
        long remainingDays = 0L;
        if (sub.getEndAt() != null && sub.getEndAt().isAfter(now)) {
            remainingDays = Duration.between(now, sub.getEndAt()).toDays();
        }

        return new MySubscriptionResponse(
                sub.getPlan() != null ? sub.getPlan().getName() : null,
                sub.getStatus(),
                sub.getStartAt(),
                sub.getEndAt(),
                Math.max(0L, remainingDays)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentTransactionResponse> getMyTransactions(Long userId, Pageable pageable) {
        Page<PaymentTransaction> page = paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        Page<PaymentTransactionResponse> mapped = page.map(tx -> new PaymentTransactionResponse(
                tx.getId(),
                tx.getProviderRefCode(),
                tx.getPlan() != null ? tx.getPlan().getName() : null,
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getPaidAt(),
                tx.getCreatedAt()
        ));
        return PageResponse.of(mapped);
    }

    private String generateUniqueRefCode(String prefix) {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return prefix + randomPart;
    }
}
