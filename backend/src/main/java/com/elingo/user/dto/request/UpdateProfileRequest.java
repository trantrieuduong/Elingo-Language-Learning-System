package com.elingo.user.dto.request;

import com.elingo.auth.annotation.ValidUsername;
import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @BuildMediaKey
        @AllowedMediaKind(MediaKind.IMAGE)
        @Size(max = 255, message = "INVALID_REQUEST")
        String avatarUrl,

        @NotBlank(message = "FULL_NAME_INVALID")
        @Size(max = 150, message = "FULL_NAME_INVALID")
        String fullName,

        @ValidUsername
        String username
) {
}

