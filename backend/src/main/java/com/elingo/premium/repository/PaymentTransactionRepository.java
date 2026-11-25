package com.elingo.premium.repository;

import com.elingo.premium.dto.projection.DateAmountCountProjection;
import com.elingo.premium.dto.projection.PlanRevenueStat;
import com.elingo.premium.dto.projection.TransactionStatusCount;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PaymentTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>, JpaSpecificationExecutor<PaymentTransaction> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
    Optional<PaymentTransaction> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "plan", "provider", "subscription"})
    @Query("SELECT t FROM PaymentTransaction t WHERE t.id = :id")
    Optional<PaymentTransaction> findDetailById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"user", "plan", "provider"})
    Page<PaymentTransaction> findAll(Specification<PaymentTransaction> spec, Pageable pageable);

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

    @Query(value = "SELECT DATE_FORMAT(t.paid_at, '%Y-%m-%d') AS periodKey, SUM(t.amount) AS totalAmount, COUNT(t.id) AS txCount " +
            "FROM payment_transactions t " +
            "WHERE t.status = 'SUCCESS' AND t.paid_at >= :from AND t.paid_at < :to " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<DateAmountCountProjection> getRevenueTimeseriesByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE_FORMAT(DATE_SUB(t.paid_at, INTERVAL WEEKDAY(t.paid_at) DAY), '%Y-%m-%d') AS periodKey, SUM(t.amount) AS totalAmount, COUNT(t.id) AS txCount " +
            "FROM payment_transactions t " +
            "WHERE t.status = 'SUCCESS' AND t.paid_at >= :from AND t.paid_at < :to " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<DateAmountCountProjection> getRevenueTimeseriesByWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE_FORMAT(t.paid_at, '%Y-%m') AS periodKey, SUM(t.amount) AS totalAmount, COUNT(t.id) AS txCount " +
            "FROM payment_transactions t " +
            "WHERE t.status = 'SUCCESS' AND t.paid_at >= :from AND t.paid_at < :to " +
            "GROUP BY periodKey ORDER BY periodKey ASC", nativeQuery = true)
    List<DateAmountCountProjection> getRevenueTimeseriesByMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT t.plan.id AS planId, t.plan.name AS planName, SUM(t.amount) AS totalAmount, COUNT(t.id) AS txCount " +
            "FROM PaymentTransaction t " +
            "WHERE t.status = 'SUCCESS' AND t.paidAt >= :from AND t.paidAt < :to " +
            "GROUP BY t.plan.id, t.plan.name ORDER BY totalAmount DESC")
    List<PlanRevenueStat> getRevenueBreakdownByPlan(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT t.status AS status, COUNT(t.id) AS countVal " +
            "FROM PaymentTransaction t " +
            "WHERE t.createdAt >= :from AND t.createdAt < :to " +
            "GROUP BY t.status")
    List<TransactionStatusCount> countTransactionsByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
