package com.elingo.premium.repository.specification;

import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.entity.PaymentWebhookLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class PaymentWebhookLogSpecifications {

    private PaymentWebhookLogSpecifications() {}

    public static Specification<PaymentWebhookLog> withFilter(AdminWebhookLogFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.providerId() != null) {
                predicates.add(cb.equal(root.get("provider").get("id"), filter.providerId()));
            }
            if (filter.transactionId() != null) {
                predicates.add(cb.equal(root.get("transaction").get("id"), filter.transactionId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.signatureValid() != null) {
                predicates.add(cb.equal(root.get("signatureValid"), filter.signatureValid()));
            }
            if (filter.receivedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("receivedAt"),
                        filter.receivedFrom().atStartOfDay()
                ));
            }
            if (filter.receivedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("receivedAt"),
                        filter.receivedTo().atTime(LocalTime.MAX)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
