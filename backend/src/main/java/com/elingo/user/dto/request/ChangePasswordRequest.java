package com.elingo.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(

        @NotBlank(message = "INVALID_REQUEST")
        String oldPassword,

        @NotBlank(message = "INVALID_REQUEST")
        String newPassword
) {}