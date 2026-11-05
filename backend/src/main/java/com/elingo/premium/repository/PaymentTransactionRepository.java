package com.elingo.premium.repository;

import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.providerRefCode = :refCode")
    Optional<PaymentTransaction> findByRefCodeForUpdate(@Param("refCode") String refCode);

    @EntityGraph(attributePaths = {"plan"})
    Page<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"plan"})
    Optional<PaymentTransaction> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE PaymentTransaction t SET t.status = :newStatus WHERE t.status = :pendingStatus AND t.createdAt <= :threshold")
    int cancelStaleTransactions(@Param("newStatus") PaymentTransactionStatus newStatus,
                                @Param("pendingStatus") PaymentTransactionStatus pendingStatus,
                                @Param("threshold") LocalDateTime threshold);
}
