package com.elingo.report.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.request.CreateReportRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import com.elingo.report.dto.response.ReportResponse;
import org.springframework.data.domain.Pageable;

public interface ReportService {
    ReportResponse create(Long userId, CreateReportRequest request);
    ReportResponse getMyReport(Long userId, Long reportId);
    PageResponse<ReportResponse> getMyReports(Long userId, Pageable pageable);

    // Admin report management
    PageResponse<AdminReportResponse> searchReportsForAdmin(AdminReportSearchRequest filter, Pageable pageable);
    AdminReportResponse getReportByIdForAdmin(Long reportId);
    AdminReportResponse resolveReport(Long adminId, Long reportId, String resolutionNote);
    AdminReportResponse rejectReport(Long adminId, Long reportId, String resolutionNote);
}
