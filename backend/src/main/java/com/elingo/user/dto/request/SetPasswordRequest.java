package com.elingo.user.dto.request;

import com.elingo.auth.annotation.ValidPassword;

public record SetPasswordRequest(
        @ValidPassword
        String newPassword
) {
}
