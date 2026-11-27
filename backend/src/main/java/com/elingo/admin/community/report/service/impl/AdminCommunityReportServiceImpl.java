package com.elingo.admin.community.report.service.impl;

import com.elingo.admin.audit.annotation.Audited;
import com.elingo.admin.audit.entity.AdminAuditAction;
import com.elingo.admin.audit.entity.AdminAuditTargetType;
import com.elingo.admin.common.util.AdminPageables;
import com.elingo.admin.community.report.dto.request.RejectReportRequest;
import com.elingo.admin.community.report.dto.request.ResolveReportRequest;
import com.elingo.admin.community.report.service.AdminCommunityReportService;
import com.elingo.common.dto.PageResponse;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import com.elingo.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ADMIN-COMMUNITY-REPORT-SERVICE")
public class AdminCommunityReportServiceImpl implements AdminCommunityReportService {

    private static final Set<String> ALLOWED_SORTS = Set.of("id", "createdAt", "resolvedAt");

    private final ReportService reportService;

    @Override
    public PageResponse<AdminReportResponse> search(AdminReportSearchRequest filter, Pageable pageable) {
        return reportService.searchReportsForAdmin(filter, AdminPageables.sanitize(pageable, ALLOWED_SORTS));
    }

    @Override
    public AdminReportResponse getById(Long reportId) {
        return reportService.getReportByIdForAdmin(reportId);
    }

    @Override
    @Audited(
            action = AdminAuditAction.REPORT_RESOLVE,
            target = AdminAuditTargetType.REPORT,
            targetId = "#reportId",
            detail = "#request != null ? #request.resolutionNote() : null"
    )
    public AdminReportResponse resolve(Long adminId, Long reportId, ResolveReportRequest request) {
        String note = request != null ? request.resolutionNote() : null;
        AdminReportResponse resolved = reportService.resolveReport(adminId, reportId, note);
        log.info("Report resolved reportId={} byAdminId={}", reportId, adminId);
        return resolved;
    }

    @Override
    @Audited(
            action = AdminAuditAction.REPORT_REJECT,
            target = AdminAuditTargetType.REPORT,
            targetId = "#reportId",
            detail = "#request.resolutionNote()"
    )
    public AdminReportResponse reject(Long adminId, Long reportId, RejectReportRequest request) {
        AdminReportResponse rejected = reportService.rejectReport(adminId, reportId, request.resolutionNote());
        log.info("Report rejected reportId={} byAdminId={}", reportId, adminId);
        return rejected;
    }
}
