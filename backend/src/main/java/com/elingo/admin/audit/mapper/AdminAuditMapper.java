package com.elingo.admin.audit.mapper;

import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminAuditMapper {
    AdminAuditLogResponse toResponse(AdminAuditLog log);
}
