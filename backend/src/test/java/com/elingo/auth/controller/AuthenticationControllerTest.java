package com.elingo.auth.controller;

import com.elingo.auth.dto.request.*;
import com.elingo.auth.dto.response.AuthenticationResponse;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.auth.service.AuthService;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
    }

    @Nested
    @DisplayName("signUp")
    class SignUpTests {
        @Test
        @DisplayName("Sign up successfully")
        void signUp_Success() throws Exception {
            RegisterRequest request = new RegisterRequest("testuser", "test@gmail.com", "Test User", "Password123@");
            UserMeResponse responseMock = new UserMeResponse(1L, "testuser", "test@gmail.com", "Test User", null, Role.USER, true, false, null, null, true, false);
            
            when(authService.register(any(RegisterRequest.class))).thenReturn(responseMock);

            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {
        @Test
        @DisplayName("Login successfully")
        void login_Success() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest("testuser", "Password123@");
            LoginResult loginResult = new LoginResult(
                    new AuthenticationResponse("accessToken"),
                    ResponseCookie.from("refresh_token", "refreshTokenValue").build()
            );

            when(authService.login(any(AuthenticationRequest.class))).thenReturn(loginResult);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                    .andExpect(header().string("Set-Cookie", "refresh_token=refreshTokenValue"));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {
        @Test
        @DisplayName("Refresh token successfully")
        void refreshToken_Success() throws Exception {
            LoginResult loginResult = new LoginResult(
                    new AuthenticationResponse("newAccessToken"),
                    ResponseCookie.from("refresh_token", "newRefreshToken").build()
            );

            when(authService.refreshToken("refreshTokenValue")).thenReturn(loginResult);

            mockMvc.perform(post("/auth/refresh")
                            .cookie(new Cookie("refresh_token", "refreshTokenValue")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"))
                    .andExpect(header().string("Set-Cookie", "refresh_token=newRefreshToken"));
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {
        @Test
        @DisplayName("Logout successfully")
        void logout_Success() throws Exception {
            ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "").maxAge(0).build();
            when(authService.createLogoutCookie()).thenReturn(clearCookie);

            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        }
    }

    @Nested
    @DisplayName("authenticateWithGoogle")
    class AuthenticateWithGoogleTests {
        @Test
        @DisplayName("Authenticate with Google successfully")
        void authenticateWithGoogle_Success() throws Exception {
            GoogleAuthRequest request = new GoogleAuthRequest("googleIdToken");
            LoginResult loginResult = new LoginResult(
                    new AuthenticationResponse("accessToken"),
                    ResponseCookie.from("refresh_token", "refreshTokenValue").build()
            );

            when(authService.authenticateWithGoogle(any(GoogleAuthRequest.class))).thenReturn(loginResult);

            mockMvc.perform(post("/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
        }
    }

    @Nested
    @DisplayName("sendResetPasswordOtp")
    class SendResetPasswordOtpTests {
        @Test
        @DisplayName("Send OTP successfully")
        void sendResetPasswordOtp_Success() throws Exception {
            SendResetPasswordOtpRequest request = new SendResetPasswordOtpRequest("test@gmail.com");

            mockMvc.perform(post("/auth/password-reset/otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).sendResetPasswordOtp(any(SendResetPasswordOtpRequest.class));
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {
        @Test
        @DisplayName("Reset password successfully")
        void resetPassword_Success() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest("test@gmail.com", "123456", "NewPassword123@");

            mockMvc.perform(post("/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).resetPassword(any(ResetPasswordRequest.class));
        }
    }

    @Nested
    @DisplayName("verifyAccount")
    class VerifyAccountTests {
        @Test
        @DisplayName("Verify account successfully")
        void verifyAccount_Success() throws Exception {
            VerifyAccountRequest request = new VerifyAccountRequest("test@gmail.com", "123456");

            mockMvc.perform(post("/auth/account-verifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).verifyAccount(any(VerifyAccountRequest.class));
        }
    }

    @Nested
    @DisplayName("resendVerificationOtp")
    class ResendVerificationOtpTests {
        @Test
        @DisplayName("Resend verification OTP successfully")
        void resendVerificationOtp_Success() throws Exception {
            ResendVerificationOtpRequest request = new ResendVerificationOtpRequest("test@gmail.com");

            mockMvc.perform(post("/auth/account-verifications/otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).resendVerificationOtp(any(ResendVerificationOtpRequest.class));
        }
    }
}
