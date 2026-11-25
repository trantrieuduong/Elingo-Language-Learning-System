package com.elingo.admin.premium.service;

import com.elingo.admin.premium.service.impl.AdminPremiumWebhookLogServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import com.elingo.premium.entity.WebhookLogStatus;
import com.elingo.premium.service.PremiumAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminPremiumWebhookLogService")
class AdminPremiumWebhookLogServiceTest {

    @Mock
    private PremiumAdminService premiumAdminService;

    @InjectMocks
    private AdminPremiumWebhookLogServiceImpl adminPremiumWebhookLogService;

    @Nested
    @DisplayName("searchWebhookLogs")
    class SearchWebhookLogs {

        @Test
        @DisplayName("should delegate search when sort field is allowed")
        void shouldSearchLogs_whenSortIsAllowed() {
            AdminWebhookLogFilterRequest filter = new AdminWebhookLogFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "receivedAt"));
            PageResponse<AdminWebhookLogResponse> expected = new PageResponse<>(
                    List.of(), 1, 20, 0L, 0, true
            );

            when(premiumAdminService.searchWebhookLogsForAdmin(filter, pageable)).thenReturn(expected);

            PageResponse<AdminWebhookLogResponse> result = adminPremiumWebhookLogService.searchWebhookLogs(filter, pageable);

            assertThat(result).isEqualTo(expected);
            verify(premiumAdminService).searchWebhookLogsForAdmin(filter, pageable);
        }

        @Test
        @DisplayName("should throw INVALID_SORT when sort field is not whitelisted")
        void shouldThrowInvalidSort_whenSortNotAllowed() {
            AdminWebhookLogFilterRequest filter = new AdminWebhookLogFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("payload"));

            assertThatThrownBy(() -> adminPremiumWebhookLogService.searchWebhookLogs(filter, pageable))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.INVALID_SORT);

            verify(premiumAdminService, never()).searchWebhookLogsForAdmin(any(), any());
        }
    }

    @Nested
    @DisplayName("getWebhookLogById")
    class GetWebhookLogById {

        @Test
        @DisplayName("should return webhook log details")
        void shouldReturnDetail_whenExists() {
            AdminWebhookLogDetailResponse detail = new AdminWebhookLogDetailResponse(
                    12L, 1L, "SEPAY", 100L, "evt_1", true, WebhookLogStatus.PROCESSED,
                    "{}", LocalDateTime.now(), LocalDateTime.now()
            );
            when(premiumAdminService.getWebhookLogByIdForAdmin(12L)).thenReturn(detail);

            AdminWebhookLogDetailResponse result = adminPremiumWebhookLogService.getWebhookLogById(12L);

            assertThat(result).isEqualTo(detail);
        }
    }
}
