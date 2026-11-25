package com.elingo.admin.premium.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.premium.service.AdminPremiumWebhookLogService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminWebhookLogFilterRequest;
import com.elingo.premium.dto.response.AdminWebhookLogDetailResponse;
import com.elingo.premium.dto.response.AdminWebhookLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AdminApiPaths.PREMIUM_WEBHOOK_LOGS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Payment Webhook Logs",
        description = "Read-only endpoints for inspecting incoming payment webhook logs"
)
public class AdminPremiumWebhookLogController {

    private final AdminPremiumWebhookLogService adminPremiumWebhookLogService;

    @GetMapping
    @Operation(
            summary = "Search payment webhook logs",
            description = "Filters webhook logs by providerId, transactionId, status, signatureValid, and received date range with pagination. Allowed sort fields: receivedAt, processedAt, id. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Webhook logs retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SearchWebhookLogsSuccessResponse",
                            summary = "Sample paginated webhook logs response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "id": 12,
                                            "providerId": 1,
                                            "providerCode": "SEPAY",
                                            "transactionId": 100,
                                            "eventId": "sepay_evt_123456",
                                            "signatureValid": true,
                                            "status": "PROCESSED",
                                            "receivedAt": "2026-11-25T10:15:29",
                                            "processedAt": "2026-11-25T10:15:30"
                                          }
                                        ],
                                        "page": 0,
                                        "size": 20,
                                        "totalElements": 1,
                                        "totalPages": 1
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid filter parameters or sort field",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "InvalidDateRangeResponse",
                                    summary = "Invalid received date range",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_DATE_RANGE",
                                                "message": "Invalid date range"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "InvalidSortResponse",
                                    summary = "Unsupported sort property",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "INVALID_SORT",
                                                "message": "Invalid sort field"
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    public ApiResponse<PageResponse<AdminWebhookLogResponse>> searchWebhookLogs(
            @ParameterObject @Valid AdminWebhookLogFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<AdminWebhookLogResponse>>builder()
                .success(true)
                .data(adminPremiumWebhookLogService.searchWebhookLogs(filter, pageable))
                .build();
    }

    @GetMapping("/{logId}")
    @Operation(
            summary = "Get webhook log details",
            description = "Retrieves full payment webhook log including raw JSON payload. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Webhook log details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetWebhookLogDetailSuccessResponse",
                            summary = "Sample webhook log detail response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 12,
                                        "providerId": 1,
                                        "providerCode": "SEPAY",
                                        "transactionId": 100,
                                        "eventId": "sepay_evt_123456",
                                        "signatureValid": true,
                                        "status": "PROCESSED",
                                        "payload": "{\\"id\\":123456,\\"transferType\\":\\"in\\",\\"transferAmount\\":99000,\\"content\\":\\"ELGO8A9B1C2D3E\\"}",
                                        "receivedAt": "2026-11-25T10:15:29",
                                        "processedAt": "2026-11-25T10:15:30"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid logId",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "InvalidWebhookLogIdResponse",
                            summary = "Invalid path variable",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "INVALID_REQUEST",
                                        "message": "Invalid request"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication token is missing or expired"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin privileges required"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - Payment webhook log does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "WebhookLogNotFoundResponse",
                            summary = "Webhook log not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PAYMENT_WEBHOOK_LOG_NOT_FOUND",
                                        "message": "Payment webhook log not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminWebhookLogDetailResponse> getWebhookLogById(
            @Parameter(description = "ID of the webhook log", example = "12", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long logId
    ) {
        return ApiResponse.<AdminWebhookLogDetailResponse>builder()
                .success(true)
                .data(adminPremiumWebhookLogService.getWebhookLogById(logId))
                .build();
    }
}
