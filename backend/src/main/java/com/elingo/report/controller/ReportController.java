package com.elingo.report.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.report.dto.request.CreateReportRequest;
import com.elingo.report.dto.response.ReportResponse;
import com.elingo.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Report Controller")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Submit a report")
    public ApiResponse<ReportResponse> create(
            @Valid @RequestBody CreateReportRequest request,
            @CurrentUserId Long userId) {
        return ApiResponse.<ReportResponse>builder()
                .success(true)
                .data(reportService.create(userId, request))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get my submitted reports")
    public ApiResponse<PageResponse<ReportResponse>> getMyReports(
            @CurrentUserId Long userId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<ReportResponse>>builder()
                .success(true)
                .data(reportService.getMyReports(userId, pageable))
                .build();
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get report details")
    public ApiResponse<ReportResponse> getMyReport(
            @PathVariable @Positive(message = "INVALID_REQUEST") Long reportId,
            @CurrentUserId Long userId) {
        return ApiResponse.<ReportResponse>builder()
                .success(true)
                .data(reportService.getMyReport(userId, reportId))
                .build();
    }
}
