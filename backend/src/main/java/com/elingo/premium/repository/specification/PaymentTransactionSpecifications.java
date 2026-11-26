package com.elingo.premium.repository.specification;

import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.entity.PaymentTransaction;
import com.elingo.premium.entity.PremiumPlan;
import com.elingo.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class PaymentTransactionSpecifications {

    private PaymentTransactionSpecifications() {}

    public static Specification<PaymentTransaction> withFilter(AdminTransactionFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.userId() != null) {
                predicates.add(cb.equal(root.get("user").get("id"), filter.userId()));
            }
            if (filter.planId() != null) {
                predicates.add(cb.equal(root.get("plan").get("id"), filter.planId()));
            }
            if (filter.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        filter.createdFrom().atStartOfDay()
                ));
            }
            if (filter.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        filter.createdTo().atTime(LocalTime.MAX)
                ));
            }
            if (filter.keyword() != null && !filter.keyword().isBlank()) {
                String pattern = "%" + filter.keyword().trim().toLowerCase() + "%";
                Join<PaymentTransaction, User> userJoin = root.join("user", JoinType.INNER);
                Join<PaymentTransaction, PremiumPlan> planJoin = root.join("plan", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("providerRefCode")), pattern),
                        cb.like(cb.lower(userJoin.get("username")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern),
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(planJoin.get("name")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
