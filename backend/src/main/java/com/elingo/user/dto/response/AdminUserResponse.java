package com.elingo.user.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response payload containing comprehensive user profile information for administration")
public record AdminUserResponse(
        @Schema(description = "Unique identifier of the user", example = "10")
        Long id,

        @Schema(description = "Unique username of the user", example = "john_doe")
        String username,

        @Schema(description = "Email address of the user", example = "john.doe@example.com")
        String email,

        @Schema(description = "Full name of the user", example = "John Doe")
        String fullName,

        @Schema(
                description = "Public CDN URL of the user avatar image",
                example = "https://cdn.elingo.com/avatars/user-10.png"
        )
        @BuildMediaUrl String avatarUrl,

        @Schema(description = "System role of the user (USER or ADMIN)", example = "USER")
        Role role,

        @Schema(description = "Whether the email address has been verified", example = "true")
        Boolean isVerified,

        @Schema(description = "Whether the user account is active (false if locked)", example = "true")
        Boolean isActive,

        @Schema(
                description = "Timestamp when the password was last changed",
                example = "2026-02-15T08:30:00"
        )
        LocalDateTime passwordChangedAt,

        @Schema(
                description = "Timestamp when the username was last updated",
                example = "2026-01-10T14:20:00"
        )
        LocalDateTime usernameChangedAt,

        @Schema(description = "Whether the user has set a local password", example = "true")
        Boolean hasPassword,

        @Schema(description = "Whether the account is linked to Google OAuth", example = "false")
        Boolean linkedGoogle,

        @Schema(
                description = "Timestamp when the user account was registered",
                example = "2026-01-01T10:00:00"
        )
        LocalDateTime createdAt,

        @Schema(
                description = "Timestamp when the user account was last updated",
                example = "2026-02-15T08:30:00"
        )
        LocalDateTime updatedAt
) {
}
