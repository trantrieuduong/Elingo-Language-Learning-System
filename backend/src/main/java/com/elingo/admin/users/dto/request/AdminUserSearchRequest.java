package com.elingo.admin.users.dto.request;

import com.elingo.user.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminUserSearchRequest(
        @Size(max = 100, message = "INVALID_KEYWORD")
        String keyword,

        Role role,

        Boolean isVerified,

        Boolean isActive,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate createdTo
) {
    @AssertTrue(message = "INVALID_DATE_RANGE")
    public boolean isValidDateRange() {
        return createdFrom == null || createdTo == null || !createdFrom.isAfter(createdTo);
    }
}
