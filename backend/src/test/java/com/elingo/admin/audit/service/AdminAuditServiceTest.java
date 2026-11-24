package com.elingo.admin.audit.service;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditLog;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.audit.mapper.AdminAuditMapper;
import com.elingo.admin.audit.repository.AdminAuditRepository;
import com.elingo.admin.audit.service.impl.AdminAuditServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AdminAuditServiceTest {

    @Mock
    private AdminAuditRepository repository;

    @Mock
    private AdminAuditMapper mapper;

    @InjectMocks
    private AdminAuditServiceImpl auditService;

    private static final Long ADMIN_ID = 99L;
    private static final AdminAuditAction ACTION = AdminAuditAction.USER_LOCK;
    private static final AdminAuditTargetType TARGET_TYPE = AdminAuditTargetType.USER;
    private static final String TARGET_ID = "12345";
    private static final String DETAIL = "Account locked due to policy violation";
    private static final String IP_ADDRESS = "192.168.1.100";

    @Nested
    @DisplayName("record")
    class RecordTests {

        @Test
        @DisplayName("Record audit log successfully with valid parameters")
        void record_Success() {
            auditService.record(ADMIN_ID, ACTION, TARGET_TYPE, TARGET_ID, DETAIL, IP_ADDRESS);

            ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
            verify(repository).save(captor.capture());

            AdminAuditLog saved = captor.getValue();
            assertThat(saved.getAdminId()).isEqualTo(ADMIN_ID);
            assertThat(saved.getAction()).isEqualTo(ACTION);
            assertThat(saved.getTargetType()).isEqualTo(TARGET_TYPE);
            assertThat(saved.getTargetId()).isEqualTo(TARGET_ID);
            assertThat(saved.getDetail()).isEqualTo(DETAIL);
            assertThat(saved.getIpAddress()).isEqualTo(IP_ADDRESS);
        }

        @Test
        @DisplayName("Record audit log successfully when nullable fields are null")
        void record_WhenNullableFieldsAreNull_ShouldSaveSuccessfully() {
            auditService.record(ADMIN_ID, ACTION, TARGET_TYPE, null, null, null);

            ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
            verify(repository).save(captor.capture());

            AdminAuditLog saved = captor.getValue();
            assertThat(saved.getAdminId()).isEqualTo(ADMIN_ID);
            assertThat(saved.getAction()).isEqualTo(ACTION);
            assertThat(saved.getTargetType()).isEqualTo(TARGET_TYPE);
            assertThat(saved.getTargetId()).isNull();
            assertThat(saved.getDetail()).isNull();
            assertThat(saved.getIpAddress()).isNull();
        }

        @Test
        @DisplayName("Truncate string values when exceeding maximum length")
        void record_WhenFieldsExceedMaxLength_ShouldTruncate() {
            String longTargetId = "A".repeat(AdminAuditLog.TARGET_ID_MAX + 20);
            String longDetail = "B".repeat(AdminAuditLog.DETAIL_MAX + 50);
            String longIp = "C".repeat(AdminAuditLog.IP_MAX + 10);

            auditService.record(ADMIN_ID, ACTION, TARGET_TYPE, longTargetId, longDetail, longIp);

            ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
            verify(repository).save(captor.capture());

            AdminAuditLog saved = captor.getValue();
            assertThat(saved.getTargetId()).hasSize(AdminAuditLog.TARGET_ID_MAX)
                    .isEqualTo("A".repeat(AdminAuditLog.TARGET_ID_MAX));
            assertThat(saved.getDetail()).hasSize(AdminAuditLog.DETAIL_MAX)
                    .isEqualTo("B".repeat(AdminAuditLog.DETAIL_MAX));
            assertThat(saved.getIpAddress()).hasSize(AdminAuditLog.IP_MAX)
                    .isEqualTo("C".repeat(AdminAuditLog.IP_MAX));
        }
    }

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("Search successfully with full filter and valid sort")
        void search_WithValidFilterAndSort_Success() {
            AdminAuditLogFilterRequest filter = new AdminAuditLogFilterRequest(
                    ADMIN_ID, ACTION, TARGET_TYPE, TARGET_ID,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now()
            );
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

            AdminAuditLog entity = AdminAuditLog.builder()
                    .id(1L)
                    .adminId(ADMIN_ID)
                    .action(ACTION)
                    .targetType(TARGET_TYPE)
                    .targetId(TARGET_ID)
                    .detail(DETAIL)
                    .ipAddress(IP_ADDRESS)
                    .build();

            AdminAuditLogResponse response = new AdminAuditLogResponse(
                    1L, ADMIN_ID, ACTION, TARGET_TYPE, TARGET_ID, DETAIL, IP_ADDRESS, LocalDateTime.now()
            );

            Page<AdminAuditLog> page = new PageImpl<>(List.of(entity), pageable, 1);
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(mapper.toResponse(entity)).thenReturn(response);

            PageResponse<AdminAuditLogResponse> result = auditService.search(filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().getFirst().id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw INVALID_SORT when sort property is not allowed")
        void search_WhenSortInvalid_ShouldThrowInvalidSort() {
            AdminAuditLogFilterRequest filter = new AdminAuditLogFilterRequest(
                    null, null, null, null, null, null
            );
            Pageable invalidSortPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "invalidColumn"));

            assertThatThrownBy(() -> auditService.search(filter, invalidSortPageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_SORT));
        }

        @Test
        @DisplayName("Preserve requested sort when sorting by allowed property")
        void search_WhenSortById_ShouldKeepRequestedSort() {
            AdminAuditLogFilterRequest filter = new AdminAuditLogFilterRequest(
                    null, null, null, null, null, null
            );
            Pageable validSortPageable = PageRequest.of(1, 15, Sort.by(Sort.Direction.ASC, "id"));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            when(repository.findAll(any(Specification.class), pageableCaptor.capture()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            auditService.search(filter, validSortPageable);

            Pageable sanitized = pageableCaptor.getValue();
            assertThat(sanitized.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
            assertThat(sanitized.getPageNumber()).isEqualTo(1);
            assertThat(sanitized.getPageSize()).isEqualTo(15);
        }

        @Test
        @DisplayName("Return empty PageResponse when no records match")
        void search_WhenEmptyResult_ShouldReturnEmptyPageResponse() {
            AdminAuditLogFilterRequest filter = new AdminAuditLogFilterRequest(
                    null, null, null, null, null, null
            );
            Pageable pageable = PageRequest.of(0, 10);

            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

            PageResponse<AdminAuditLogResponse> result = auditService.search(filter, pageable);

            assertThat(result.totalElements()).isZero();
            assertThat(result.content()).isEmpty();
            assertThat(result.totalPages()).isZero();
        }
    }
}
