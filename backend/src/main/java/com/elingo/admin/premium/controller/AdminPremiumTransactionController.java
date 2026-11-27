package com.elingo.admin.premium.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.premium.service.AdminPremiumTransactionService;
import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.premium.dto.request.AdminTransactionFilterRequest;
import com.elingo.premium.dto.request.TransactionReasonRequest;
import com.elingo.premium.dto.response.AdminTransactionDetailResponse;
import com.elingo.premium.dto.response.AdminTransactionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AdminApiPaths.PREMIUM_TRANSACTIONS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Premium Transactions",
        description = "Endpoints for searching, inspecting, manually confirming, and refunding payment transactions"
)
public class AdminPremiumTransactionController {

    private final AdminPremiumTransactionService adminPremiumTransactionService;

    @GetMapping
    @Operation(
            summary = "Search payment transactions",
            description = "Filters payment transactions by keyword, status, planId, userId, and creation date range with pagination. Allowed sort fields: createdAt, paidAt, amount, id. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "SearchTransactionsSuccessResponse",
                            summary = "Sample paginated transactions response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "content": [
                                          {
                                            "id": 100,
                                            "providerRefCode": "ELGO8A9B1C2D3E",
                                            "userId": 10,
                                            "username": "john_doe",
                                            "userEmail": "john.doe@example.com",
                                            "planId": 1,
                                            "planName": "Pro Monthly",
                                            "providerCode": "SEPAY",
                                            "amount": 99000.00,
                                            "currency": "VND",
                                            "status": "SUCCESS",
                                            "paidAt": "2026-11-25T10:15:30",
                                            "createdAt": "2026-11-25T10:10:00"
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
                                    summary = "Invalid date range filter",
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
    public ApiResponse<PageResponse<AdminTransactionResponse>> searchTransactions(
            @ParameterObject @Valid AdminTransactionFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<AdminTransactionResponse>>builder()
                .success(true)
                .data(adminPremiumTransactionService.searchTransactions(filter, pageable))
                .build();
    }

    @GetMapping("/{transactionId}")
    @Operation(
            summary = "Get transaction details",
            description = "Retrieves full payment transaction details including user, plan, provider, subscription, and metadata. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Transaction details retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetTransactionDetailSuccessResponse",
                            summary = "Sample transaction detail response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 100,
                                        "providerRefCode": "ELGO8A9B1C2D3E",
                                        "userId": 10,
                                        "username": "john_doe",
                                        "userEmail": "john.doe@example.com",
                                        "userFullName": "John Doe",
                                        "planId": 1,
                                        "planName": "Pro Monthly",
                                        "planDurationDays": 30,
                                        "providerCode": "SEPAY",
                                        "providerName": "SePay",
                                        "subscriptionId": 55,
                                        "subscriptionStatus": "ACTIVE",
                                        "subscriptionStartAt": "2026-11-25T10:15:30",
                                        "subscriptionEndAt": "2026-12-25T10:15:30",
                                        "amount": 99000.00,
                                        "currency": "VND",
                                        "status": "SUCCESS",
                                        "metadata": null,
                                        "paidAt": "2026-11-25T10:15:30",
                                        "createdAt": "2026-11-25T10:10:00",
                                        "updatedAt": "2026-11-25T10:15:30"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid transactionId",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "InvalidTransactionIdResponse",
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
            description = "Not Found - Payment transaction does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "TransactionNotFoundResponse",
                            summary = "Transaction not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PAYMENT_TRANSACTION_NOT_FOUND",
                                        "message": "Payment transaction not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminTransactionDetailResponse> getTransactionById(
            @Parameter(description = "ID of the payment transaction", example = "100", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long transactionId
    ) {
        return ApiResponse.<AdminTransactionDetailResponse>builder()
                .success(true)
                .data(adminPremiumTransactionService.getTransactionById(transactionId))
                .build();
    }

    @PutMapping("/{transactionId}/confirm")
    @Operation(
            summary = "Manually confirm pending transaction",
            description = "Marks a PENDING transaction as SUCCESS and activates/extends the user's premium subscription. Records audit action TRANSACTION_MANUAL_CONFIRM. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reason for manual confirmation",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TransactionReasonRequest.class),
                    examples = @ExampleObject(
                            name = "ManualConfirmRequestExample",
                            summary = "Sample manual confirmation payload",
                            value = """
                                    {
                                      "reason": "Verified manual bank transfer statement #FT261125001"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Transaction manually confirmed successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ManualConfirmSuccessResponse",
                            summary = "Sample confirmed transaction response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 100,
                                        "providerRefCode": "ELGO8A9B1C2D3E",
                                        "userId": 10,
                                        "username": "john_doe",
                                        "userEmail": "john.doe@example.com",
                                        "userFullName": "John Doe",
                                        "planId": 1,
                                        "planName": "Pro Monthly",
                                        "planDurationDays": 30,
                                        "providerCode": null,
                                        "providerName": null,
                                        "subscriptionId": 55,
                                        "subscriptionStatus": "ACTIVE",
                                        "subscriptionStartAt": "2026-11-25T18:45:00",
                                        "subscriptionEndAt": "2026-12-25T18:45:00",
                                        "amount": 99000.00,
                                        "currency": "VND",
                                        "status": "SUCCESS",
                                        "metadata": "{\\"manualConfirmed\\":true,\\"confirmedByAdminId\\":1,\\"confirmReason\\":\\"Verified manual bank transfer statement #FT261125001\\",\\"confirmedAt\\":\\"2026-11-25T18:45:00\\"}",
                                        "paidAt": "2026-11-25T18:45:00",
                                        "createdAt": "2026-11-25T10:10:00",
                                        "updatedAt": "2026-11-25T18:45:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation error or transaction already processed",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "TransactionAlreadyProcessedResponse",
                            summary = "Transaction is not PENDING",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PAYMENT_TRANSACTION_ALREADY_PROCESSED",
                                        "message": "Payment transaction has already been completed or processed"
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
            description = "Not Found - Payment transaction does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "ConfirmTransactionNotFoundResponse",
                            summary = "Transaction not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PAYMENT_TRANSACTION_NOT_FOUND",
                                        "message": "Payment transaction not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminTransactionDetailResponse> confirmManually(
            @CurrentUserId Long adminId,
            @Parameter(description = "ID of the PENDING payment transaction", example = "100", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long transactionId,
            @Valid @RequestBody TransactionReasonRequest request
    ) {
        return ApiResponse.<AdminTransactionDetailResponse>builder()
                .success(true)
                .data(adminPremiumTransactionService.confirmManually(adminId, transactionId, request))
                .build();
    }

    @PutMapping("/{transactionId}/refund")
    @Operation(
            summary = "Refund successful transaction",
            description = "Marks a SUCCESS transaction as REFUNDED and revokes the remaining unused subscription time calculated from paidAt. Only transactions within the configured refund window (app.premium.refund.max-days, default 3 days) can be refunded. Records audit action TRANSACTION_REFUND. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Reason for refunding the transaction",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TransactionReasonRequest.class),
                    examples = @ExampleObject(
                            name = "RefundTransactionRequestExample",
                            summary = "Sample refund transaction payload",
                            value = """
                                    {
                                      "reason": "Customer requested refund within 24 hours via support ticket #SUP-409"
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Transaction refunded successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "RefundSuccessResponse",
                            summary = "Sample refunded transaction response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 100,
                                        "providerRefCode": "ELGO8A9B1C2D3E",
                                        "userId": 10,
                                        "username": "john_doe",
                                        "userEmail": "john.doe@example.com",
                                        "userFullName": "John Doe",
                                        "planId": 1,
                                        "planName": "Pro Monthly",
                                        "planDurationDays": 30,
                                        "providerCode": "SEPAY",
                                        "providerName": "SePay",
                                        "subscriptionId": 55,
                                        "subscriptionStatus": "CANCELLED",
                                        "subscriptionStartAt": "2026-11-24T18:45:00",
                                        "subscriptionEndAt": "2026-11-25T18:45:00",
                                        "amount": 99000.00,
                                        "currency": "VND",
                                        "status": "REFUNDED",
                                        "metadata": "{\\"refunded\\":true,\\"refundedByAdminId\\":1,\\"refundReason\\":\\"Customer requested refund within 24 hours via support ticket #SUP-409\\",\\"refundedAt\\":\\"2026-11-25T18:45:00\\",\\"revokedMinutes\\":41760}",
                                        "paidAt": "2026-11-24T18:45:00",
                                        "createdAt": "2026-11-24T18:40:00",
                                        "updatedAt": "2026-11-25T18:45:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation error, transaction not refundable (PAYMENT_TRANSACTION_NOT_REFUNDABLE), or refund window expired (PAYMENT_TRANSACTION_REFUND_EXPIRED)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "TransactionNotRefundableResponse",
                                    summary = "Transaction is not SUCCESS",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "PAYMENT_TRANSACTION_NOT_REFUNDABLE",
                                                "message": "Only successful payment transactions can be refunded"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "TransactionRefundExpiredResponse",
                                    summary = "Transaction exceeded allowed refund window",
                                    value = """
                                            {
                                              "success": false,
                                              "error": {
                                                "code": "PAYMENT_TRANSACTION_REFUND_EXPIRED",
                                                "message": "Payment transaction has exceeded the allowed refund window"
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Not Found - Payment transaction does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "RefundTransactionNotFoundResponse",
                            summary = "Transaction not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PAYMENT_TRANSACTION_NOT_FOUND",
                                        "message": "Payment transaction not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminTransactionDetailResponse> refundTransaction(
            @CurrentUserId Long adminId,
            @Parameter(description = "ID of the SUCCESS payment transaction", example = "100", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long transactionId,
            @Valid @RequestBody TransactionReasonRequest request
    ) {
        return ApiResponse.<AdminTransactionDetailResponse>builder()
                .success(true)
                .data(adminPremiumTransactionService.refundTransaction(adminId, transactionId, request))
                .build();
    }
}
