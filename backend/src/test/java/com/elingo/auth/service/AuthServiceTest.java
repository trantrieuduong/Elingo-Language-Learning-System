package com.elingo.auth.service;

import com.elingo.auth.dto.request.*;
import com.elingo.auth.dto.response.LoginResult;
import com.elingo.auth.service.impl.AuthServiceImpl;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.util.EmailTemplateName;
import com.elingo.common.util.OtpType;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.mapper.UserMapper;
import com.elingo.user.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
        @DisplayName("Authenticate with Google successfully")
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
    }
}
