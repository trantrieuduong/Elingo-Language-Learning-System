package com.elingo.premium.repository;

import com.elingo.premium.entity.PaymentWebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long>, JpaSpecificationExecutor<PaymentWebhookLog> {
    Optional<PaymentWebhookLog> findByProviderIdAndEventId(Long providerId, String eventId);

    boolean existsByProviderIdAndEventId(Long providerId, String eventId);

    @EntityGraph(attributePaths = {"provider", "transaction"})
    @Query("SELECT w FROM PaymentWebhookLog w WHERE w.id = :id")
    Optional<PaymentWebhookLog> findDetailById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"provider", "transaction"})
    Page<PaymentWebhookLog> findAll(Specification<PaymentWebhookLog> spec, Pageable pageable);
}

