package com.elingo.report.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.report.dto.request.CreateReportRequest;
import com.elingo.report.dto.response.ReportResponse;
import org.springframework.data.domain.Pageable;

public interface ReportService {
    ReportResponse create(Long userId, CreateReportRequest request);
    ReportResponse getMyReport(Long userId, Long reportId);
    PageResponse<ReportResponse> getMyReports(Long userId, Pageable pageable);
}
