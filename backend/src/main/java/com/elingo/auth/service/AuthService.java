package com.elingo.auth.service;

import com.elingo.auth.dto.request.AuthenticationRequest;
import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.auth.dto.request.ResetPasswordRequest;
import com.elingo.auth.dto.request.SendResetPasswordOtpRequest;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.user.dto.response.UserResponse;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    LoginResult login(AuthenticationRequest request);

    LoginResult refreshToken(String refreshToken);

    ResponseCookie createLogoutCookie();

    void sendResetPasswordOtp(SendResetPasswordOtpRequest request);

    void resetPassword(ResetPasswordRequest request);
}

