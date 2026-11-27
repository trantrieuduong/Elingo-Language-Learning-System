package com.elingo.admin.community.report.service;

import com.elingo.admin.community.report.dto.request.RejectReportRequest;
import com.elingo.admin.community.report.dto.request.ResolveReportRequest;
import com.elingo.common.dto.PageResponse;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import org.springframework.data.domain.Pageable;

public interface AdminCommunityReportService {

    PageResponse<AdminReportResponse> search(AdminReportSearchRequest filter, Pageable pageable);

    AdminReportResponse getById(Long reportId);

    AdminReportResponse resolve(Long adminId, Long reportId, ResolveReportRequest request);

    AdminReportResponse reject(Long adminId, Long reportId, RejectReportRequest request);
}
