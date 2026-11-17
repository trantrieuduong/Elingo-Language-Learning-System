package com.elingo.admin.audit.dto.request;

import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Bộ lọc tìm audit log. Mọi trường đều tùy chọn.
 * from/to theo ISO-8601, ví dụ 2026-11-01T00:00:00 (giờ server, UTC+7).
 */
public record AdminAuditLogFilterRequest(
        @Positive(message = "INVALID_REQUEST")
        Long adminId,
        AdminAuditAction action,
        AdminAuditTargetType targetType,
        @Size(max = 64, message = "INVALID_REQUEST")
        String targetId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to

) {
    @AssertTrue(message = "INVALID_REQUEST")
    public boolean isValidDateRange() {
        return from == null || to == null || !from.isAfter(to);
    }
}
