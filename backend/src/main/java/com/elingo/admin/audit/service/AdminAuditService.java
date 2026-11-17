package com.elingo.admin.audit.service;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AdminAuditService {

    /**
     * Ghi một dòng audit log. Chạy trong transaction hiện tại (nếu có),
     * nên rollback thao tác chính thì log cũng rollback.
     *
     * @param targetId  ID đối tượng, có thể null
     * @param detail    lý do hoặc giá trị trước/sau, có thể null (tự cắt nếu quá dài)
     * @param ipAddress IP của admin, có thể null
     */
    void record(Long adminId,
                AdminAuditAction action,
                AdminAuditTargetType targetType,
                String targetId,
                String detail,
                String ipAddress);

    PageResponse<AdminAuditLogResponse> search(AdminAuditLogFilterRequest filter, Pageable pageable);
}
