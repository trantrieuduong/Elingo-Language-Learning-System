package com.elingo.user.service;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.util.EmailTemplateName;
import com.elingo.common.util.OtpType;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.dto.response.UserPublicResponse;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.mapper.UserMapper;
import com.elingo.user.repository.UserRepository;
import com.elingo.user.service.impl.UserServiceImpl;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String EMAIL = "old@gmail.com";
    private static final String NEW_EMAIL = "new@gmail.com";
    private static final String PASSWORD_HASH = "$2a$10$hashedOldPassword";
    private static final String OLD_PASSWORD = "OldPassword123@";
    private static final String NEW_PASSWORD = "NewPassword123@";
    private static final String NEW_PASSWORD_HASH = "$2a$10$hashedNewPassword";
    private static final String OTP = "123456";
    private static final String FULL_NAME = "Test User";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username(USERNAME)
                .email(EMAIL)
                .passwordHash(PASSWORD_HASH)
                .fullName(FULL_NAME)
                .role(Role.USER)
                .isActive(true)
                .isVerified(true)
                .build();
    }

    // ========================================================================
    // changePassword
    // ========================================================================
    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("Change password successfully")
        void changePassword_Success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(OLD_PASSWORD, PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, PASSWORD_HASH)).thenReturn(false);
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

            ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD);
            userService.changePassword(USER_ID, request);

            assertThat(testUser.getPasswordHash()).isEqualTo(NEW_PASSWORD_HASH);
            assertThat(testUser.getPasswordChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Change password failed: User not found")
        void changePassword_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD);

            assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("Change password failed: Old password incorrect")
        void changePassword_Fail_OldPasswordIncorrect() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(OLD_PASSWORD, PASSWORD_HASH)).thenReturn(false);

            ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD);

            assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.OLD_PASSWORD_INCORRECT));
        }

        @Test
        @DisplayName("Change password failed: New password same as old")
        void changePassword_Fail_NewPasswordSameAsOld() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(OLD_PASSWORD, PASSWORD_HASH)).thenReturn(true);
            when(passwordEncoder.matches(NEW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

            ChangePasswordRequest request = new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD);

            assertThatThrownBy(() -> userService.changePassword(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.NEW_PASSWORD_SAME_AS_OLD));
        }
    }

    // ========================================================================
    // setPassword
    // ========================================================================
    @Nested
    @DisplayName("setPassword")
    class SetPasswordTests {

        @Test
        @DisplayName("Set password successfully for account without password")
        void setPassword_Success() {
            testUser.setPasswordHash(null);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

            SetPasswordRequest request = new SetPasswordRequest(NEW_PASSWORD);
            userService.setPassword(USER_ID, request);

            assertThat(testUser.getPasswordHash()).isEqualTo(NEW_PASSWORD_HASH);
            assertThat(testUser.getPasswordChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Set password failed: User not found")
        void setPassword_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            SetPasswordRequest request = new SetPasswordRequest(NEW_PASSWORD);

            assertThatThrownBy(() -> userService.setPassword(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("Set password failed: Password already set")
        void setPassword_Fail_PasswordAlreadySet() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            SetPasswordRequest request = new SetPasswordRequest(NEW_PASSWORD);

            assertThatThrownBy(() -> userService.setPassword(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PASSWORD_ALREADY_SET));
        }
    }

    // ========================================================================
    // sendOTPUpdateEmail
    // ========================================================================
    @Nested
    @DisplayName("sendOTPUpdateEmail")
    class SendOTPUpdateEmailTests {

        @Test
        @DisplayName("Send OTP update email successfully")
        void sendOTPUpdateEmail_Success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
            when(otpService.generateAndSaveOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL)).thenReturn(OTP);

            SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(NEW_EMAIL);
            userService.sendOTPUpdateEmail(USER_ID, request);

            verify(otpService).generateAndSaveOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL);
            verify(emailService).sendEmail(
                    eq(NEW_EMAIL),
                    eq(USERNAME),
                    eq(EmailTemplateName.SEND_OTP),
                    eq(OTP),
                    anyString()
            );
        }

        @Test
        @DisplayName("Send OTP failed: User not found")
        void sendOTPUpdateEmail_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(NEW_EMAIL);

            assertThatThrownBy(() -> userService.sendOTPUpdateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(emailService, never()).sendEmail(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Send OTP failed: Email unchanged")
        void sendOTPUpdateEmail_Fail_EmailUnchanged() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            // Gửi email trùng với email hiện tại
            SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(EMAIL);

            assertThatThrownBy(() -> userService.sendOTPUpdateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.EMAIL_UNCHANGED));

            verify(emailService, never()).sendEmail(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Send OTP failed: Email already existed")
        void sendOTPUpdateEmail_Fail_EmailExisted() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

            SendOTPUpdateEmailRequest request = new SendOTPUpdateEmailRequest(NEW_EMAIL);

            assertThatThrownBy(() -> userService.sendOTPUpdateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.EMAIL_EXISTED));

            verify(emailService, never()).sendEmail(any(), any(), any(), any(), any());
        }
    }

    // ========================================================================
    // updateEmail
    // ========================================================================
    @Nested
    @DisplayName("updateEmail")
    class UpdateEmailTests {

        @Test
        @DisplayName("Update email successfully")
        void updateEmail_Success() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
            when(passwordEncoder.matches(OLD_PASSWORD, PASSWORD_HASH)).thenReturn(true);

            UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, OLD_PASSWORD);
            userService.updateEmail(USER_ID, request);

            assertThat(testUser.getEmail()).isEqualTo(NEW_EMAIL);
            assertThat(testUser.getGoogleProviderId()).isNull();
            verify(otpService).verifyOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL, OTP);
        }

        @Test
        @DisplayName("Update email failed: User not found")
        void updateEmail_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, OLD_PASSWORD);

            assertThatThrownBy(() -> userService.updateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("Update email failed: Email already existed")
        void updateEmail_Fail_EmailExisted() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

            UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, OLD_PASSWORD);

            assertThatThrownBy(() -> userService.updateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.EMAIL_EXISTED));
        }

        @Test
        @DisplayName("Update email failed: Password incorrect")
        void updateEmail_Fail_PasswordIncorrect() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
            when(passwordEncoder.matches("WrongPassword123@", PASSWORD_HASH)).thenReturn(false);

            UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, OTP, "WrongPassword123@");

            assertThatThrownBy(() -> userService.updateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PASSWORD_INCORRECT));
        }

        @Test
        @DisplayName("Update email failed: OTP invalid")
        void updateEmail_Fail_OtpInvalid() {
            String wrongOtp = "000000";
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
            when(passwordEncoder.matches(OLD_PASSWORD, PASSWORD_HASH)).thenReturn(true);
            doThrow(new AppException(AppError.OTP_INVALID))
                    .when(otpService).verifyOtp(OtpType.CHANGE_EMAIL, NEW_EMAIL, wrongOtp);

            UpdateEmailRequest request = new UpdateEmailRequest(NEW_EMAIL, wrongOtp, OLD_PASSWORD);

            assertThatThrownBy(() -> userService.updateEmail(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.OTP_INVALID));
        }
    }

    // ========================================================================
    // getMyInfo
    // ========================================================================
    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfoTests {

        @Test
        @DisplayName("Get my info successfully")
        void getMyInfo_Success() {
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null,
                    Role.USER, true, true, null, null, true, false
            );
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UserMeResponse result = userService.getMyInfo(USER_ID);

            assertThat(result).isEqualTo(expectedResponse);
            assertThat(result.id()).isEqualTo(USER_ID);
            assertThat(result.email()).isEqualTo(EMAIL);
            verify(userMapper).toUserMeResponse(testUser);
        }

        @Test
        @DisplayName("Get my info failed: User not found")
        void getMyInfo_Fail_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyInfo(USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }
    }

    // ========================================================================
    // getUserById
    // ========================================================================
    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {

        @Test
        @DisplayName("Get user by id (self): Returns UserMeResponse")
        void getUserById_Self_ReturnsUserMeResponse() {
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null,
                    Role.USER, true, true, null, null, true, false
            );
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            Object result = userService.getUserById(USER_ID, USER_ID);

            assertThat(result).isInstanceOf(UserMeResponse.class);
            assertThat(result).isEqualTo(expectedResponse);
            verify(userMapper).toUserMeResponse(testUser);
        }

        @Test
        @DisplayName("Get user by id (other user): Returns UserPublicResponse")
        void getUserById_OtherUser_ReturnsUserPublicResponse() {
            Long otherUserId = 2L;
            UserPublicResponse expectedResponse = new UserPublicResponse(
                    USER_ID, USERNAME, FULL_NAME, null, Role.USER
            );
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserPublicResponse(testUser)).thenReturn(expectedResponse);

            Object result = userService.getUserById(USER_ID, otherUserId);

            assertThat(result).isInstanceOf(UserPublicResponse.class);
            assertThat(result).isEqualTo(expectedResponse);
            verify(userMapper).toUserPublicResponse(testUser);
        }

        @Test
        @DisplayName("Get user by id failed: User not found")
        void getUserById_Fail_UserNotFound() {
            Long targetUserId = 999L;
            when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(targetUserId, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }
    }
}
