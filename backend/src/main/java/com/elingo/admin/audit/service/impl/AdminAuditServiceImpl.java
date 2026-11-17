package com.elingo.admin.audit.service.impl;

import com.elingo.admin.audit.dto.request.AdminAuditLogFilterRequest;
import com.elingo.admin.audit.dto.response.AdminAuditLogResponse;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditLog;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.audit.mapper.AdminAuditMapper;
import com.elingo.admin.audit.repository.AdminAuditRepository;
import com.elingo.admin.audit.repository.AdminAuditSpecifications;
import com.elingo.admin.audit.service.AdminAuditService;
import com.elingo.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j(topic = "ADMIN-AUDIT-SERVICE")
@Service
@RequiredArgsConstructor
public class AdminAuditServiceImpl implements AdminAuditService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "id");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt", "id");


    private final AdminAuditRepository repository;
    private final AdminAuditMapper mapper;

    @Override
    @Transactional
    public void record(Long adminId,
                       AdminAuditAction action,
                       AdminAuditTargetType targetType,
                       String targetId,
                       String detail,
                       String ipAddress) {
        AdminAuditLog entry = AdminAuditLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(truncate(targetId, AdminAuditLog.TARGET_ID_MAX))
                .detail(truncate(detail, AdminAuditLog.DETAIL_MAX))
                .ipAddress(truncate(ipAddress, AdminAuditLog.IP_MAX))
                .build();

        repository.save(entry);

        log.info("Admin audit adminId={} action={} targetType={} targetId={}",
                adminId, action, targetType, entry.getTargetId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> search(AdminAuditLogFilterRequest filter, Pageable pageable) {
        return PageResponse.of(repository
                .findAll(AdminAuditSpecifications.from(filter), sanitize(pageable))
                .map(mapper::toResponse));
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private Pageable sanitize(Pageable pageable) {
        boolean valid = pageable.getSort().isSorted()
                && pageable.getSort().stream().allMatch(o -> SORTABLE.contains(o.getProperty()));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                valid ? pageable.getSort() : DEFAULT_SORT);
    }
}
