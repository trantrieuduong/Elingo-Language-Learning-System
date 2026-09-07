package com.elingo.auth.dto.response;

import org.springframework.http.ResponseCookie;

public record LoginResult(
        AuthenticationResponse response,
        ResponseCookie refreshCookie
) { }
