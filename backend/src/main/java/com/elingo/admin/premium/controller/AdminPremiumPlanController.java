package com.elingo.admin.premium.controller;

import com.elingo.admin.common.constant.AdminApiPaths;
import com.elingo.admin.premium.service.AdminPremiumPlanService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.premium.dto.request.CreatePremiumPlanRequest;
import com.elingo.premium.dto.request.UpdatePremiumPlanRequest;
import com.elingo.premium.dto.response.AdminPremiumPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping(AdminApiPaths.PREMIUM_PLANS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(
        name = "Admin - Premium Plans",
        description = "Management endpoints for premium subscription plans in the admin console"
)
public class AdminPremiumPlanController {

    private final AdminPremiumPlanService adminPremiumPlanService;

    @GetMapping
    @Operation(
            summary = "Get all premium plans",
            description = "Retrieves all premium plans including disabled ones without pagination. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Premium plans retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetAllPlansSuccessResponse",
                            summary = "Sample list of premium plans",
                            value = """
                                    {
                                      "success": true,
                                      "data": [
                                        {
                                          "id": 1,
                                          "name": "Pro Monthly",
                                          "price": 99000.00,
                                          "currency": "VND",
                                          "durationDays": 30,
                                          "isActive": true,
                                          "createdAt": "2026-01-01T10:00:00",
                                          "updatedAt": "2026-01-01T10:00:00"
                                        },
                                        {
                                          "id": 2,
                                          "name": "Pro Yearly (Legacy)",
                                          "price": 899000.00,
                                          "currency": "VND",
                                          "durationDays": 365,
                                          "isActive": false,
                                          "createdAt": "2026-01-01T10:00:00",
                                          "updatedAt": "2026-02-10T09:30:00"
                                        }
                                      ]
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
    public ApiResponse<List<AdminPremiumPlanResponse>> getAllPlans() {
        return ApiResponse.<List<AdminPremiumPlanResponse>>builder()
                .success(true)
                .data(adminPremiumPlanService.getAllPlans())
                .build();
    }

    @GetMapping("/{planId}")
    @Operation(
            summary = "Get premium plan details",
            description = "Retrieves detailed information of a specific premium plan by ID. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Premium plan retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "GetPlanByIdSuccessResponse",
                            summary = "Sample premium plan details",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "name": "Pro Monthly",
                                        "price": 99000.00,
                                        "currency": "VND",
                                        "durationDays": 30,
                                        "isActive": true,
                                        "createdAt": "2026-01-01T10:00:00",
                                        "updatedAt": "2026-01-01T10:00:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid planId",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "InvalidPlanIdResponse",
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
            description = "Not Found - Premium plan does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "PremiumPlanNotFoundResponse",
                            summary = "Plan not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PREMIUM_PLAN_NOT_FOUND",
                                        "message": "Premium plan not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminPremiumPlanResponse> getPlanById(
            @Parameter(description = "ID of the premium plan", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long planId
    ) {
        return ApiResponse.<AdminPremiumPlanResponse>builder()
                .success(true)
                .data(adminPremiumPlanService.getPlanById(planId))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create premium plan",
            description = "Creates a new premium plan with isActive defaulted to false. Records audit action PLAN_CREATE. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Premium plan creation payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreatePremiumPlanRequest.class),
                    examples = @ExampleObject(
                            name = "CreatePremiumPlanRequestExample",
                            summary = "Sample create premium plan request",
                            value = """
                                    {
                                      "name": "Pro Quarterly",
                                      "price": 249000.00,
                                      "currency": "VND",
                                      "durationDays": 90
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Premium plan created successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "CreatePlanSuccessResponse",
                            summary = "Sample created premium plan response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 3,
                                        "name": "Pro Quarterly",
                                        "price": 249000.00,
                                        "currency": "VND",
                                        "durationDays": 90,
                                        "isActive": false,
                                        "createdAt": "2026-11-25T18:30:00",
                                        "updatedAt": "2026-11-25T18:30:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation error",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "InvalidCreatePlanRequestResponse",
                            summary = "Validation error",
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
    public ApiResponse<AdminPremiumPlanResponse> createPlan(
            @Valid @RequestBody CreatePremiumPlanRequest request
    ) {
        return ApiResponse.<AdminPremiumPlanResponse>builder()
                .success(true)
                .data(adminPremiumPlanService.createPlan(request))
                .build();
    }

    @PutMapping("/{planId}")
    @Operation(
            summary = "Update premium plan",
            description = "Updates plan name and price. Price changes apply only to future purchases; existing subscriptions and past transactions remain unchanged. Records audit action PLAN_UPDATE. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Premium plan update payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UpdatePremiumPlanRequest.class),
                    examples = @ExampleObject(
                            name = "UpdatePremiumPlanRequestExample",
                            summary = "Sample update premium plan request",
                            value = """
                                    {
                                      "name": "Pro Monthly (Promo)",
                                      "price": 89000.00
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Premium plan updated successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdatePlanSuccessResponse",
                            summary = "Sample updated premium plan response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "name": "Pro Monthly (Promo)",
                                        "price": 89000.00,
                                        "currency": "VND",
                                        "durationDays": 30,
                                        "isActive": true,
                                        "createdAt": "2026-01-01T10:00:00",
                                        "updatedAt": "2026-11-25T18:35:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Validation error",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "InvalidUpdatePlanRequestResponse",
                            summary = "Validation error",
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
            description = "Not Found - Premium plan does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "UpdatePlanNotFoundResponse",
                            summary = "Plan not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PREMIUM_PLAN_NOT_FOUND",
                                        "message": "Premium plan not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminPremiumPlanResponse> updatePlan(
            @Parameter(description = "ID of the premium plan to update", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long planId,
            @Valid @RequestBody UpdatePremiumPlanRequest request
    ) {
        return ApiResponse.<AdminPremiumPlanResponse>builder()
                .success(true)
                .data(adminPremiumPlanService.updatePlan(planId, request))
                .build();
    }

    @PutMapping("/{planId}/disable")
    @Operation(
            summary = "Disable premium plan",
            description = "Stops selling the premium plan without cancelling existing PENDING transactions so bank transfers can still be activated by webhook. Records audit action PLAN_DISABLE. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Premium plan disabled successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DisablePlanSuccessResponse",
                            summary = "Sample disabled plan response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "name": "Pro Monthly",
                                        "price": 99000.00,
                                        "currency": "VND",
                                        "durationDays": 30,
                                        "isActive": false,
                                        "createdAt": "2026-01-01T10:00:00",
                                        "updatedAt": "2026-11-25T18:40:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid planId or plan already inactive",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "PlanAlreadyInactiveResponse",
                            summary = "Plan already inactive",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PREMIUM_PLAN_ALREADY_INACTIVE",
                                        "message": "Premium plan is already inactive"
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
            description = "Not Found - Premium plan does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "DisablePlanNotFoundResponse",
                            summary = "Plan not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PREMIUM_PLAN_NOT_FOUND",
                                        "message": "Premium plan not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminPremiumPlanResponse> disablePlan(
            @Parameter(description = "ID of the premium plan to disable", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long planId
    ) {
        return ApiResponse.<AdminPremiumPlanResponse>builder()
                .success(true)
                .data(adminPremiumPlanService.disablePlan(planId))
                .build();
    }

    @PutMapping("/{planId}/enable")
    @Operation(
            summary = "Enable premium plan",
            description = "Re-enables a disabled premium plan for purchase. Records audit action PLAN_ENABLE. Requires ADMIN role."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Premium plan enabled successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "EnablePlanSuccessResponse",
                            summary = "Sample enabled plan response",
                            value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "id": 1,
                                        "name": "Pro Monthly",
                                        "price": 99000.00,
                                        "currency": "VND",
                                        "durationDays": 30,
                                        "isActive": true,
                                        "createdAt": "2026-01-01T10:00:00",
                                        "updatedAt": "2026-11-25T18:42:00"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid planId or plan already active",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "PlanAlreadyActiveResponse",
                            summary = "Plan already active",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PREMIUM_PLAN_ALREADY_ACTIVE",
                                        "message": "Premium plan is already active"
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
            description = "Not Found - Premium plan does not exist",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            name = "EnablePlanNotFoundResponse",
                            summary = "Plan not found error",
                            value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "PREMIUM_PLAN_NOT_FOUND",
                                        "message": "Premium plan not found"
                                      }
                                    }
                                    """
                    )
            )
    )
    public ApiResponse<AdminPremiumPlanResponse> enablePlan(
            @Parameter(description = "ID of the premium plan to enable", example = "1", required = true)
            @PathVariable @Positive(message = "INVALID_REQUEST") Long planId
    ) {
        return ApiResponse.<AdminPremiumPlanResponse>builder()
                .success(true)
                .data(adminPremiumPlanService.enablePlan(planId))
                .build();
    }
}
