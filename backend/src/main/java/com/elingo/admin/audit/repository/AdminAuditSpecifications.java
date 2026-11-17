package com.elingo.admin.audit.repository;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.entity.AdminAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AdminAuditSpecifications {

    private AdminAuditSpecifications() {}

    public static Specification<AdminAuditLog> from(AdminAuditLogFilterRequest request) {
        return (root, query, cb) -> {
            if (request == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (request.adminId() != null) {
                predicates.add(cb.equal(root.get("adminId"), request.adminId()));
            }
            if (request.action() != null) {
                predicates.add(cb.equal(root.get("action"), request.action()));
            }
            if (request.targetType() != null) {
                predicates.add(cb.equal(root.get("targetType"), request.targetType()));
            }
            if (request.targetId() != null && !request.targetId().isBlank()) {
                predicates.add(cb.equal(root.get("targetId"), request.targetId().trim()));
            }
            if (request.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.from()));
            }
            if (request.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.to()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
