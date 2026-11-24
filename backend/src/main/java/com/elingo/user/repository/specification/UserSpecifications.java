package com.elingo.user.repository.specification;

import com.elingo.user.dto.request.AdminUserSearchRequest;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;

public final class UserSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private UserSpecifications() {}

    public static Specification<User> from(AdminUserSearchRequest request) {
        if (request == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return Specification.allOf(
                keywordContains(request.keyword()),
                hasRole(request.role()),
                isVerified(request.isVerified()),
                isActive(request.isActive()),
                createdFrom(request.createdFrom()),
                createdTo(request.createdTo())
        );
    }

    private static Specification<User> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("fullName")), pattern, LIKE_ESCAPE)
            );
        };
    }

    private static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    private static Specification<User> isVerified(Boolean verified) {
        return (root, query, cb) -> verified == null ? null : cb.equal(root.get("isVerified"), verified);
    }

    private static Specification<User> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("isActive"), active);
    }

    private static Specification<User> createdFrom(LocalDate from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    private static Specification<User> createdTo(LocalDate to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(LocalTime.MAX));
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
