package com.elingo.premium.repository;

import com.elingo.premium.entity.PaymentWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {
    Optional<PaymentWebhookLog> findByProviderIdAndEventId(Long providerId, String eventId);

    boolean existsByProviderIdAndEventId(Long providerId, String eventId);
}

