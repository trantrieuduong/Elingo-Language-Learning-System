package com.elingo.admin.premium.service.impl;

import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.premium.service.AdminPremiumWebhookLogService;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import com.elingo.premium.service.PremiumAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPremiumWebhookLogServiceImpl implements AdminPremiumWebhookLogService {

    private static final Set<String> ALLOWED_SORTS = Set.of("receivedAt", "processedAt", "id");

    private final PremiumAdminService premiumAdminService;

    @Override
    public PageResponse<AdminWebhookLogResponse> searchWebhookLogs(
            AdminWebhookLogFilterRequest filter,
            Pageable pageable
    ) {
        return premiumAdminService.searchWebhookLogsForAdmin(
                filter,
                AdminPageables.sanitize(pageable, ALLOWED_SORTS)
        );
    }

    @Override
    public AdminWebhookLogDetailResponse getWebhookLogById(Long logId) {
        return premiumAdminService.getWebhookLogByIdForAdmin(logId);
    }
}
