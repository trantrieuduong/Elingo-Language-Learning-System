package com.elingo.user.dto.request;

import com.elingo.auth.annotation.ValidUsername;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 150, message = "FULL_NAME_INVALID") 
        @Pattern(regexp = ".*\\S.*", message = "FULL_NAME_INVALID")
        // @Pattern bỏ qua null (PATCH semantics), chặn blank/whitespace-only string.
        String fullName,

        @ValidUsername 
        String username
) {
}
