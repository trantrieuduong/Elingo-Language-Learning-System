package com.elingo.auth.service;

import com.elingo.auth.dto.request.AuthenticationRequest;
import com.elingo.auth.dto.request.GoogleAuthRequest;
import com.elingo.auth.dto.request.RegisterRequest;
import com.elingo.auth.dto.request.ResendVerificationOtpRequest;
import com.elingo.auth.dto.request.ResetPasswordRequest;
import com.elingo.auth.dto.request.SendResetPasswordOtpRequest;
import com.elingo.auth.dto.request.VerifyAccountRequest;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.auth.service.impl.AuthServiceImpl;
import com.elingo.common.enums.EmailTemplateName;
import com.elingo.common.enums.OtpType;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.mapper.UserMapper;
import com.elingo.user.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpService otpService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "test@gmail.com";
    private static final String PASSWORD = "Password123@";
    private static final String PASSWORD_HASH = "hashedPassword";
    private static final String OTP = "123456";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenDays", 14);
        ReflectionTestUtils.setField(authService, "contextPath", "/api/v1");

        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email(EMAIL)
                .passwordHash(PASSWORD_HASH)
                .role(Role.USER)
                .isActive(true)
                .isVerified(true)
                .build();
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {
        @Test
        @DisplayName("Register successfully")
        void register_Success() {
            RegisterRequest request = new RegisterRequest(USERNAME, EMAIL, "Test User", PASSWORD);
            when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userMapper.toUser(request)).thenReturn(testUser);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
            when(otpService.generateAndSaveOtp(OtpType.VERIFY_ACCOUNT, EMAIL)).thenReturn(OTP);
            UserMeResponse responseMock = new UserMeResponse(USER_ID, USERNAME, EMAIL, "Test User", null, Role.USER, true, false, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(responseMock);

            UserMeResponse response = authService.register(request);

            assertThat(response).isEqualTo(responseMock);
            verify(userRepository).save(testUser);
            verify(emailService).sendEmail(eq(EMAIL), eq(USERNAME), eq(EmailTemplateName.SEND_OTP), eq(OTP), anyString());
        }

        @Test
        @DisplayName("Register failed: Username existed")
        void register_Fail_UsernameExisted() {
            RegisterRequest request = new RegisterRequest(USERNAME, EMAIL, "Test User", PASSWORD);
            when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USERNAME_EXISTED));
        }

        @Test
        @DisplayName("Register failed: Email existed")
        void register_Fail_EmailExisted() {
            RegisterRequest request = new RegisterRequest(USERNAME, EMAIL, "Test User", PASSWORD);
            when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.EMAIL_EXISTED));
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {
        @Test
        @DisplayName("Login successfully")
        void login_Success() {
            AuthenticationRequest request = new AuthenticationRequest(USERNAME, PASSWORD);
            when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), testUser.getRole().name())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("refreshToken");

            LoginResult result = authService.login(request);

            assertThat(result.response().accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshCookie().getValue()).isEqualTo("refreshToken");
        }

        @Test
        @DisplayName("Login failed: User not found")
        void login_Fail_UserNotFound() {
            AuthenticationRequest request = new AuthenticationRequest(USERNAME, PASSWORD);
            when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_CREDENTIALS));

            verifyNoInteractions(passwordEncoder);
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Login failed: Invalid credentials")
        void login_Fail_InvalidCredentials() {
            AuthenticationRequest request = new AuthenticationRequest(USERNAME, PASSWORD);
            when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_CREDENTIALS));
        }

        @Test
        @DisplayName("Login failed: User inactive")
        void login_Fail_UserInactive() {
            testUser.setIsActive(false);
            AuthenticationRequest request = new AuthenticationRequest(USERNAME, PASSWORD);
            when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Login failed: User not verified")
        void login_Fail_UserNotVerified() {
            testUser.setIsVerified(false);
            AuthenticationRequest request = new AuthenticationRequest(USERNAME, PASSWORD);
            when(userRepository.findByUsernameOrEmail(USERNAME, USERNAME)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
            when(otpService.generateAndSaveOtp(OtpType.VERIFY_ACCOUNT, EMAIL)).thenReturn(OTP);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_VERIFIED));

            verify(emailService).sendEmail(eq(EMAIL), eq(USERNAME), eq(EmailTemplateName.SEND_OTP), eq(OTP), anyString());
        }
    }

    @Nested
    @DisplayName("authenticateWithGoogle")
    class AuthenticateWithGoogleTests {
        @Test
        @DisplayName("Authenticate with Google successfully: existing Google user")
        void authenticateWithGoogle_Success() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(EMAIL);
            payload.setSubject("google123");

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), testUser.getRole().name())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("refreshToken");

            LoginResult result = authService.authenticateWithGoogle(request);

            assertThat(result.response().accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshCookie().getValue()).isEqualTo("refreshToken");
        }

        @Test
        @DisplayName("Authenticate with Google failed: existing user is inactive")
        void authenticateWithGoogle_Fail_UserInactive() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(EMAIL);
            payload.setSubject("google123");

            testUser.setIsActive(false);

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.authenticateWithGoogle(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Authenticate with Google successfully: link existing unverified user by email and reset password")
        void authenticateWithGoogle_Success_LinkExistingUser_Unverified() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(EMAIL);
            payload.setSubject("google123");

            testUser.setIsVerified(false);
            testUser.setPasswordHash(PASSWORD_HASH);

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), testUser.getRole().name())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("refreshToken");

            LoginResult result = authService.authenticateWithGoogle(request);

            assertThat(result.response().accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshCookie().getValue()).isEqualTo("refreshToken");
            assertThat(testUser.getPasswordHash()).isNull();
            assertThat(testUser.getGoogleProviderId()).isEqualTo("google123");
            assertThat(testUser.getIsVerified()).isTrue();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Authenticate with Google successfully: link existing verified user by email without resetting password")
        void authenticateWithGoogle_Success_LinkExistingUser_AlreadyVerified() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(EMAIL);
            payload.setSubject("google123");

            testUser.setIsVerified(true);
            testUser.setPasswordHash(PASSWORD_HASH);

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), testUser.getRole().name())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("refreshToken");

            LoginResult result = authService.authenticateWithGoogle(request);

            assertThat(result.response().accessToken()).isEqualTo("accessToken");
            assertThat(testUser.getPasswordHash()).isEqualTo(PASSWORD_HASH);
            assertThat(testUser.getGoogleProviderId()).isEqualTo("google123");
            assertThat(testUser.getIsVerified()).isTrue();
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Authenticate with Google failed: linked user is inactive")
        void authenticateWithGoogle_Fail_LinkExistingUser_Inactive() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(EMAIL);
            payload.setSubject("google123");

            testUser.setIsActive(false);

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
            when(userRepository.save(testUser)).thenReturn(testUser);

            assertThatThrownBy(() -> authService.authenticateWithGoogle(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));

            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Authenticate with Google successfully: create new user when not found by googleProviderId or email (with name)")
        void authenticateWithGoogle_Success_CreateNewUser_WithName() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail(EMAIL);
            payload.setSubject("google123");
            payload.set("name", "Google Name");

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(USER_ID);
                return saved;
            });
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), Role.USER.name())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("refreshToken");

            LoginResult result = authService.authenticateWithGoogle(request);

            assertThat(result.response().accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshCookie().getValue()).isEqualTo("refreshToken");
            verify(userRepository).save(argThat(u ->
                    EMAIL.equals(u.getEmail()) &&
                    "Google Name".equals(u.getFullName()) &&
                    "google123".equals(u.getGoogleProviderId()) &&
                    Boolean.TRUE.equals(u.getIsVerified()) &&
                    u.getUsername() != null && u.getUsername().startsWith("u")
            ));
        }

        @Test
        @DisplayName("Authenticate with Google successfully: create new user with fallback name when name is null")
        void authenticateWithGoogle_Success_CreateNewUser_WithoutName() {
            GoogleAuthRequest request = new GoogleAuthRequest("googleToken");
            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setEmail("john.doe@gmail.com");
            payload.setSubject("google123");

            when(googleTokenVerifierService.verify("googleToken")).thenReturn(payload);
            when(userRepository.findByGoogleProviderId("google123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("john.doe@gmail.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User saved = invocation.getArgument(0);
                saved.setId(USER_ID);
                return saved;
            });
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), Role.USER.name())).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("refreshToken");

            LoginResult result = authService.authenticateWithGoogle(request);

            assertThat(result.response().accessToken()).isEqualTo("accessToken");
            verify(userRepository).save(argThat(u -> "john.doe".equals(u.getFullName())));
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {
        private static final String REFRESH_TOKEN = "validRefreshToken";

        @Test
        @DisplayName("Refresh token successfully")
        @SuppressWarnings("unchecked")
        void refreshToken_Success() {
            Jws<Claims> jwsMock = mock(Jws.class);
            Claims claimsMock = mock(Claims.class);

            when(jwtService.getClaimsJws(REFRESH_TOKEN)).thenReturn(jwsMock);
            when(jwsMock.getBody()).thenReturn(claimsMock);
            when(claimsMock.get("token_type", String.class)).thenReturn("REFRESH");
            when(claimsMock.getSubject()).thenReturn(String.valueOf(USER_ID));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(String.valueOf(USER_ID), testUser.getRole().name())).thenReturn("newAccessToken");
            when(jwtService.generateRefreshToken(String.valueOf(USER_ID))).thenReturn("newRefreshToken");

            LoginResult result = authService.refreshToken(REFRESH_TOKEN);

            assertThat(result.response().accessToken()).isEqualTo("newAccessToken");
            assertThat(result.refreshCookie().getValue()).isEqualTo("newRefreshToken");
            assertThat(result.refreshCookie().getName()).isEqualTo("refresh_token");
            assertThat(result.refreshCookie().isHttpOnly()).isTrue();
            assertThat(result.refreshCookie().isSecure()).isFalse();
            assertThat(result.refreshCookie().getPath()).isEqualTo("/api/v1/auth");
            assertThat(result.refreshCookie().getMaxAge()).isEqualTo(Duration.ofDays(14));
        }

        @Test
        @DisplayName("Refresh token failed: Token is null")
        void refreshToken_Fail_NullToken() {
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REFRESH_TOKEN_INVALID));

            verifyNoInteractions(jwtService);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Refresh token failed: Token is blank")
        void refreshToken_Fail_BlankToken() {
            assertThatThrownBy(() -> authService.refreshToken("   "))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REFRESH_TOKEN_INVALID));

            verifyNoInteractions(jwtService);
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Refresh token failed: JwtException when parsing token")
        void refreshToken_Fail_JwtException() {
            when(jwtService.getClaimsJws(REFRESH_TOKEN)).thenThrow(new JwtException("Invalid token"));

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REFRESH_TOKEN_INVALID));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Refresh token failed: Wrong token type")
        @SuppressWarnings("unchecked")
        void refreshToken_Fail_WrongTokenType() {
            Jws<Claims> jwsMock = mock(Jws.class);
            Claims claimsMock = mock(Claims.class);

            when(jwtService.getClaimsJws(REFRESH_TOKEN)).thenReturn(jwsMock);
            when(jwsMock.getBody()).thenReturn(claimsMock);
            when(claimsMock.get("token_type", String.class)).thenReturn("ACCESS");

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REFRESH_TOKEN_INVALID));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Refresh token failed: Null token type in claims")
        @SuppressWarnings("unchecked")
        void refreshToken_Fail_NullTokenType() {
            Jws<Claims> jwsMock = mock(Jws.class);
            Claims claimsMock = mock(Claims.class);

            when(jwtService.getClaimsJws(REFRESH_TOKEN)).thenReturn(jwsMock);
            when(jwsMock.getBody()).thenReturn(claimsMock);
            when(claimsMock.get("token_type", String.class)).thenReturn(null);

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.REFRESH_TOKEN_INVALID));

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Refresh token failed: User not found")
        @SuppressWarnings("unchecked")
        void refreshToken_Fail_UserNotFound() {
            Jws<Claims> jwsMock = mock(Jws.class);
            Claims claimsMock = mock(Claims.class);

            when(jwtService.getClaimsJws(REFRESH_TOKEN)).thenReturn(jwsMock);
            when(jwsMock.getBody()).thenReturn(claimsMock);
            when(claimsMock.get("token_type", String.class)).thenReturn("REFRESH");
            when(claimsMock.getSubject()).thenReturn(String.valueOf(USER_ID));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        }

        @Test
        @DisplayName("Refresh token failed: User inactive")
        @SuppressWarnings("unchecked")
        void refreshToken_Fail_UserInactive() {
            Jws<Claims> jwsMock = mock(Jws.class);
            Claims claimsMock = mock(Claims.class);

            testUser.setIsActive(false);

            when(jwtService.getClaimsJws(REFRESH_TOKEN)).thenReturn(jwsMock);
            when(jwsMock.getBody()).thenReturn(claimsMock);
            when(claimsMock.get("token_type", String.class)).thenReturn("REFRESH");
            when(claimsMock.getSubject()).thenReturn(String.valueOf(USER_ID));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.refreshToken(REFRESH_TOKEN))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));

            verify(jwtService, never()).generateAccessToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("createLogoutCookie")
    class CreateLogoutCookieTests {
        @Test
        @DisplayName("Create logout cookie successfully")
        void createLogoutCookie_Success() {
            ResponseCookie cookie = authService.createLogoutCookie();

            assertThat(cookie.getName()).isEqualTo("refresh_token");
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.isSecure()).isFalse();
            assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        }

        @Test
        @DisplayName("Create logout cookie with contextPath ending with slash")
        void createLogoutCookie_WhenContextPathEndsWithSlash() {
            ReflectionTestUtils.setField(authService, "contextPath", "/api/v1/");

            ResponseCookie cookie = authService.createLogoutCookie();

            assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        }
    }

    @Nested
    @DisplayName("sendResetPasswordOtp")
    class SendResetPasswordOtpTests {
        @Test
        @DisplayName("Send reset password OTP successfully")
        void sendResetPasswordOtp_Success() {
            SendResetPasswordOtpRequest request = new SendResetPasswordOtpRequest(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
            when(otpService.generateAndSaveOtp(OtpType.RESET_PASSWORD, EMAIL)).thenReturn(OTP);

            authService.sendResetPasswordOtp(request);

            verify(emailService).sendEmail(
                    eq(EMAIL),
                    eq(USERNAME),
                    eq(EmailTemplateName.SEND_OTP),
                    eq(OTP),
                    eq(OtpType.RESET_PASSWORD.getTitle())
            );
        }

        @Test
        @DisplayName("Send reset password OTP failed: Email not existed")
        void sendResetPasswordOtp_Fail_EmailNotExisted() {
            SendResetPasswordOtpRequest request = new SendResetPasswordOtpRequest(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.sendResetPasswordOtp(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.EMAIL_NOT_EXISTED));

            verifyNoInteractions(otpService);
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {
        @Test
        @DisplayName("Reset password successfully")
        void resetPassword_Success() {
            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP, "NewPassword123@");
            doNothing().when(otpService).verifyOtp(OtpType.RESET_PASSWORD, EMAIL, OTP);
            when(passwordEncoder.encode("NewPassword123@")).thenReturn("newHashedPassword");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

            authService.resetPassword(request);

            assertThat(testUser.getPasswordHash()).isEqualTo("newHashedPassword");
            assertThat(testUser.getPasswordChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Reset password failed: Invalid or expired OTP")
        void resetPassword_Fail_OtpInvalid() {
            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP, "NewPassword123@");
            doThrow(new AppException(AppError.OTP_INVALID))
                    .when(otpService).verifyOtp(OtpType.RESET_PASSWORD, EMAIL, OTP);

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.OTP_INVALID));

            verifyNoInteractions(passwordEncoder);
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Reset password failed: Email not existed")
        void resetPassword_Fail_EmailNotExisted() {
            ResetPasswordRequest request = new ResetPasswordRequest(EMAIL, OTP, "NewPassword123@");
            doNothing().when(otpService).verifyOtp(OtpType.RESET_PASSWORD, EMAIL, OTP);
            when(passwordEncoder.encode("NewPassword123@")).thenReturn("newHashedPassword");
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.EMAIL_NOT_EXISTED));
        }
    }

    @Nested
    @DisplayName("verifyAccount")
    class VerifyAccountTests {
        @Test
        @DisplayName("Verify account successfully")
        void verifyAccount_Success() {
            VerifyAccountRequest request = new VerifyAccountRequest(EMAIL, OTP);
            testUser.setIsVerified(false);
            when(userRepository.findByUsernameOrEmail(EMAIL, EMAIL)).thenReturn(Optional.of(testUser));
            doNothing().when(otpService).verifyOtp(OtpType.VERIFY_ACCOUNT, EMAIL, OTP);

            authService.verifyAccount(request);

            assertThat(testUser.getIsVerified()).isTrue();
        }

        @Test
        @DisplayName("Verify account failed: User not found")
        void verifyAccount_Fail_UserNotFound() {
            VerifyAccountRequest request = new VerifyAccountRequest(EMAIL, OTP);
            when(userRepository.findByUsernameOrEmail(EMAIL, EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyAccount(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verifyNoInteractions(otpService);
        }

        @Test
        @DisplayName("Verify account failed: OTP invalid")
        void verifyAccount_Fail_OtpInvalid() {
            VerifyAccountRequest request = new VerifyAccountRequest(EMAIL, OTP);
            testUser.setIsVerified(false);
            when(userRepository.findByUsernameOrEmail(EMAIL, EMAIL)).thenReturn(Optional.of(testUser));
            doThrow(new AppException(AppError.OTP_INVALID))
                    .when(otpService).verifyOtp(OtpType.VERIFY_ACCOUNT, EMAIL, OTP);

            assertThatThrownBy(() -> authService.verifyAccount(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.OTP_INVALID));

            assertThat(testUser.getIsVerified()).isFalse();
        }
    }

    @Nested
    @DisplayName("resendVerificationOtp")
    class ResendVerificationOtpTests {
        @Test
        @DisplayName("Resend verification OTP successfully when user is unverified")
        void resendVerificationOtp_Success() {
            ResendVerificationOtpRequest request = new ResendVerificationOtpRequest(EMAIL);
            testUser.setIsVerified(false);
            when(userRepository.findByUsernameOrEmail(EMAIL, EMAIL)).thenReturn(Optional.of(testUser));
            when(otpService.generateAndSaveOtp(OtpType.VERIFY_ACCOUNT, EMAIL)).thenReturn(OTP);

            authService.resendVerificationOtp(request);

            verify(emailService).sendEmail(
                    eq(EMAIL),
                    eq(USERNAME),
                    eq(EmailTemplateName.SEND_OTP),
                    eq(OTP),
                    eq(OtpType.VERIFY_ACCOUNT.getTitle())
            );
        }

        @Test
        @DisplayName("Resend verification OTP skipped when user is already verified")
        void resendVerificationOtp_Skip_AlreadyVerified() {
            ResendVerificationOtpRequest request = new ResendVerificationOtpRequest(EMAIL);
            testUser.setIsVerified(true);
            when(userRepository.findByUsernameOrEmail(EMAIL, EMAIL)).thenReturn(Optional.of(testUser));

            authService.resendVerificationOtp(request);

            verifyNoInteractions(otpService);
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Resend verification OTP failed: User not found")
        void resendVerificationOtp_Fail_UserNotFound() {
            ResendVerificationOtpRequest request = new ResendVerificationOtpRequest(EMAIL);
            when(userRepository.findByUsernameOrEmail(EMAIL, EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendVerificationOtp(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));

            verifyNoInteractions(otpService);
            verifyNoInteractions(emailService);
        }
    }
}
