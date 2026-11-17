package com.elingo.admin.audit.repository;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditLog;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
@SuppressWarnings({"rawtypes", "unchecked"})
class AdminAuditSpecificationsTest {

    @Mock
    private Root<AdminAuditLog> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Predicate predicate;

    @Nested
    @DisplayName("from")
    class FromTests {

        @Test
        @DisplayName("Return conjunction predicate when filter request is null")
        void from_WhenRequestIsNull_ShouldReturnConjunction() {
            Specification<AdminAuditLog> spec = AdminAuditSpecifications.from(null);
            when(cb.conjunction()).thenReturn(predicate);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
            verify(cb).conjunction();
        }

        @Test
        @DisplayName("Create all predicates when all filter criteria are provided")
        void from_WhenAllFieldsPresent_ShouldCreateAllPredicates() {
            Long adminId = 1L;
            AdminAuditAction action = AdminAuditAction.USER_LOCK;
            AdminAuditTargetType targetType = AdminAuditTargetType.USER;
            String targetId = "123";
            LocalDateTime from = LocalDateTime.now().minusDays(1);
            LocalDateTime to = LocalDateTime.now();

            AdminAuditLogFilterRequest request = new AdminAuditLogFilterRequest(
                    adminId, action, targetType, targetId, from, to
            );

            Path adminIdPath = mock(Path.class);
            Path actionPath = mock(Path.class);
            Path targetTypePath = mock(Path.class);
            Path targetIdPath = mock(Path.class);
            Path createdAtPath = mock(Path.class);

            when(root.get("adminId")).thenReturn(adminIdPath);
            when(root.get("action")).thenReturn(actionPath);
            when(root.get("targetType")).thenReturn(targetTypePath);
            when(root.get("targetId")).thenReturn(targetIdPath);
            when(root.get("createdAt")).thenReturn(createdAtPath);

            when(cb.equal(adminIdPath, adminId)).thenReturn(mock(Predicate.class));
            when(cb.equal(actionPath, action)).thenReturn(mock(Predicate.class));
            when(cb.equal(targetTypePath, targetType)).thenReturn(mock(Predicate.class));
            when(cb.equal(targetIdPath, targetId)).thenReturn(mock(Predicate.class));
            when(cb.greaterThanOrEqualTo((Expression) createdAtPath, from)).thenReturn(mock(Predicate.class));
            when(cb.lessThanOrEqualTo((Expression) createdAtPath, to)).thenReturn(mock(Predicate.class));
            doReturn(predicate).when(cb).and(any(Predicate[].class));

            Specification<AdminAuditLog> spec = AdminAuditSpecifications.from(request);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
            verify(cb).equal(adminIdPath, adminId);
            verify(cb).equal(actionPath, action);
            verify(cb).equal(targetTypePath, targetType);
            verify(cb).equal(targetIdPath, targetId);
            verify(cb).greaterThanOrEqualTo((Expression) createdAtPath, from);
            verify(cb).lessThanOrEqualTo((Expression) createdAtPath, to);
        }

        @Test
        @DisplayName("Trim targetId before creating equal predicate")
        void from_WhenTargetIdHasWhitespace_ShouldTrimPredicate() {
            AdminAuditLogFilterRequest request = new AdminAuditLogFilterRequest(
                    null, null, null, "   trimmedId   ", null, null
            );

            Path targetIdPath = mock(Path.class);
            when(root.get("targetId")).thenReturn(targetIdPath);
            when(cb.equal(targetIdPath, "trimmedId")).thenReturn(mock(Predicate.class));
            doReturn(predicate).when(cb).and(any(Predicate[].class));

            Specification<AdminAuditLog> spec = AdminAuditSpecifications.from(request);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
            verify(cb).equal(targetIdPath, "trimmedId");
        }

        @Test
        @DisplayName("Ignore targetId predicate when targetId contains only whitespace")
        void from_WhenTargetIdIsBlank_ShouldNotBuildPredicate() {
            AdminAuditLogFilterRequest request = new AdminAuditLogFilterRequest(
                    null, null, null, "    ", null, null
            );

            doReturn(predicate).when(cb).and(any(Predicate[].class));

            Specification<AdminAuditLog> spec = AdminAuditSpecifications.from(request);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
            verify(root, never()).get("targetId");
            verify(cb, never()).equal(any(), any());
        }

        @Test
        @DisplayName("Return empty and predicate array when all filter fields are null")
        void from_WhenAllFieldsNull_ShouldReturnAndWithEmptyArray() {
            AdminAuditLogFilterRequest request = new AdminAuditLogFilterRequest(
                    null, null, null, null, null, null
            );

            doReturn(predicate).when(cb).and(any(Predicate[].class));

            Specification<AdminAuditLog> spec = AdminAuditSpecifications.from(request);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isSameAs(predicate);
            verify(cb).and(any(Predicate[].class));
        }
    }
}
