package com.elingo.user.dto.request;

import com.elingo.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "Request query parameters for searching and filtering users in the admin console")
public record AdminUserSearchRequest(
        @Schema(
                description = "Keyword to search across username, email, or full name",
                example = "john",
                maxLength = 100
        )
        @Size(max = 100, message = "INVALID_KEYWORD")
        String keyword,

        @Schema(
                description = "Filter by user account role",
                example = "USER"
        )
        Role role,

        @Schema(
                description = "Filter by email verification status",
                example = "true"
        )
        Boolean isVerified,

        @Schema(
                description = "Filter by account active/lock status (true = active, false = locked)",
                example = "true"
        )
        Boolean isActive,

        @Schema(
                description = "Filter users registered on or after this date (ISO format: YYYY-MM-DD)",
                example = "2026-01-01"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @Schema(
                description = "Filter users registered on or before this date (ISO format: YYYY-MM-DD)",
                example = "2026-12-31"
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdTo
) {
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isValidDateRange() {
        return createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo);
    }
}
