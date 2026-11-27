package com.elingo.admin.community.report.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.community.report.dto.request.RejectReportRequest;
import com.elingo.admin.community.report.dto.request.ResolveReportRequest;
import com.elingo.admin.community.report.service.AdminCommunityReportService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.report.dto.request.AdminReportSearchRequest;
import com.elingo.report.dto.response.AdminReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AdminApiPaths.COMMUNITY_REPORTS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Community Reports",
        description = "Management endpoints for viewing and processing user reports in the admin console"
)
public class AdminCommunityReportController {

    private final AdminCommunityReportService adminCommunityReportService;

    @GetMapping
    @Operation(
            summary = "Search and filter reports",
            description = "Retrieves a paginated list of user reports filtered by status, target type, reporter ID, target ID, and creation date range. Allowed sort fields: id, createdAt, resolvedAt."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Paginated list of reports retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST, INVALID_DATE_RANGE, or INVALID_SORT",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<PageResponse<AdminReportResponse>> searchReports(
            @Valid @ParameterObject AdminReportSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<AdminReportResponse>>builder()
                .success(true)
                .data(adminCommunityReportService.search(filter, pageable))
                .build();
    }

    @GetMapping("/{reportId}")
    @Operation(
            summary = "Get report detail",
            description = "Retrieves full details of a specific report including reporter, evidence images, and resolution metadata."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Report retrieved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid reportId parameter",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - REPORT_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminReportResponse> getReportById(
            @Parameter(description = "Identifier of the report", example = "42", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long reportId) {
        return ApiResponse.<AdminReportResponse>builder()
                .success(true)
                .data(adminCommunityReportService.getById(reportId))
                .build();
    }

    @PutMapping("/{reportId}/resolve")
    @Operation(
            summary = "Resolve a pending report",
            description = "Marks a PENDING report as RESOLVED, records the resolving administrator and note, sends a REPORT_RESOLVED notification to the reporter, and records audit action REPORT_RESOLVE."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Report resolved successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST or REPORT_ALREADY_RESOLVED",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - REPORT_NOT_FOUND or USER_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminReportResponse> resolveReport(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the report to resolve", example = "42", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long reportId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Optional resolution note",
                    required = false,
                    content = @Content(schema = @Schema(implementation = ResolveReportRequest.class))
            )
            @Valid @RequestBody(required = false) ResolveReportRequest request) {
        return ApiResponse.<AdminReportResponse>builder()
                .success(true)
                .data(adminCommunityReportService.resolve(adminId, reportId, request))
                .build();
    }

    @PutMapping("/{reportId}/reject")
    @Operation(
            summary = "Reject a pending report",
            description = "Marks a PENDING report as REJECTED with a mandatory administrative reason and records audit action REPORT_REJECT."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Report rejected successfully"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - INVALID_REQUEST or REPORT_ALREADY_RESOLVED",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - REPORT_NOT_FOUND or USER_NOT_FOUND",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    public ApiResponse<AdminReportResponse> rejectReport(
            @CurrentUserId Long adminId,
            @Parameter(description = "Identifier of the report to reject", example = "42", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long reportId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Rejection reason",
                    required = true,
                    content = @Content(schema = @Schema(implementation = RejectReportRequest.class))
            )
            @Valid @RequestBody RejectReportRequest request) {
        return ApiResponse.<AdminReportResponse>builder()
                .success(true)
                .data(adminCommunityReportService.reject(adminId, reportId, request))
                .build();
    }
}
