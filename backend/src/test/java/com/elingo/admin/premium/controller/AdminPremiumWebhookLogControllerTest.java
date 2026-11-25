package com.elingo.admin.premium.controller;

import com.elingo.admin.premium.service.AdminPremiumWebhookLogService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import com.elingo.premium.entity.WebhookLogStatus;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminPremiumWebhookLogController")
class AdminPremiumWebhookLogControllerTest {

    @Mock
    private AdminPremiumWebhookLogService adminPremiumWebhookLogService;

    @InjectMocks
    private AdminPremiumWebhookLogController controller;

    @Nested
    @DisplayName("searchWebhookLogs")
    class SearchWebhookLogs {

        @Test
        @DisplayName("should return paginated webhook logs")
        void shouldSearchWebhookLogs() {
            AdminWebhookLogFilterRequest filter = new AdminWebhookLogFilterRequest(null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<AdminWebhookLogResponse> page = new PageResponse<>(
                    List.of(), 1, 20, 0L, 0, true
            );

            when(adminPremiumWebhookLogService.searchWebhookLogs(filter, pageable)).thenReturn(page);

            ApiResponse<PageResponse<AdminWebhookLogResponse>> response = controller.searchWebhookLogs(filter, pageable);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            verify(adminPremiumWebhookLogService).searchWebhookLogs(filter, pageable);
        }
    }

    @Nested
    @DisplayName("getWebhookLogById")
    class GetWebhookLogById {

        @Test
        @DisplayName("should return webhook log details")
        void shouldGetWebhookLogById() {
            AdminWebhookLogDetailResponse detail = new AdminWebhookLogDetailResponse(
                    12L, 1L, "SEPAY", 100L, "evt_1", true, WebhookLogStatus.PROCESSED,
                    "{}", LocalDateTime.now(), LocalDateTime.now()
            );
            when(adminPremiumWebhookLogService.getWebhookLogById(12L)).thenReturn(detail);

            ApiResponse<AdminWebhookLogDetailResponse> response = controller.getWebhookLogById(12L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(detail);
            verify(adminPremiumWebhookLogService).getWebhookLogById(12L);
        }
    }
}
