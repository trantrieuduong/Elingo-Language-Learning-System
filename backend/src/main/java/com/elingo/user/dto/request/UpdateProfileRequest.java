package com.elingo.user.dto.request;

import com.elingo.auth.annotation.ValidUsername;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        String avatarUrl,

        @NotBlank(message = "FULL_NAME_INVALID") 
        @Size(max = 150, message = "FULL_NAME_INVALID") 
        String fullName,

        @ValidUsername 
        String username
) {
}
