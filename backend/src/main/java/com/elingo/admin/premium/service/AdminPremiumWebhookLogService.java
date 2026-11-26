package com.elingo.admin.premium.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import org.springframework.data.domain.Pageable;

public interface AdminPremiumWebhookLogService {
    PageResponse<AdminWebhookLogResponse> searchWebhookLogs(AdminWebhookLogFilterRequest filter, Pageable pageable);
    AdminWebhookLogDetailResponse getWebhookLogById(Long logId);
}
