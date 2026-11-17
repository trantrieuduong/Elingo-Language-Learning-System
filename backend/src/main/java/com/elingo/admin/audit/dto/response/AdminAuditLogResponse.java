package com.elingo.admin.audit.dto.response;

import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;

import java.time.LocalDateTime;

public record AdminAuditLogResponse(
        Long id,
        Long adminId,
        AdminAuditAction action,
        AdminAuditTargetType targetType,
        String targetId,
        String detail,
        String ipAddress,
        LocalDateTime createdAt
) {
}
