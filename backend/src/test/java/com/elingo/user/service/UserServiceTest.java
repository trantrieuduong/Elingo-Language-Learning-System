package com.elingo.user.service;

import com.elingo.user.dto.request.AdminUserSearchRequest;
import com.elingo.user.dto.response.AdminUserResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.enums.EmailTemplateName;
import com.elingo.common.enums.OtpType;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

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

    @Mock
    private ApplicationEventPublisher publisher;

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
    private static final String OLD_UPLOADS_KEY = "uploads/1/old.png";
    private static final String NEW_VERIFIED_KEY = "verified/1/new.png";
    private static final String NEW_UPLOADS_KEY = "uploads/1/new.png";

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
                    anyString());
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
                    Role.USER, true, true, null, null, true, false);
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
                    Role.USER, true, true, null, null, true, false);
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
                    USER_ID, USERNAME, FULL_NAME, null);
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

    // ========================================================================
    // updateProfile
    // ========================================================================
    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Update profile successfully: only fullName changed")
        void updateProfile_Success_UpdateFullNameOnly() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, "New Name", null, Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                UpdateProfileRequest req = invocation.getArgument(1);
                if (req.fullName() != null)
                    u.setFullName(req.fullName());
                return null;
            }).when(userMapper).updateUser(eq(testUser), any(UpdateProfileRequest.class));

            UpdateProfileRequest request = new UpdateProfileRequest(null, "New Name", USERNAME);
            UserMeResponse result = userService.updateProfile(USER_ID, request);

            assertThat(testUser.getUsernameChangedAt()).isNull(); // username not touched
            assertThat(result).isEqualTo(expectedResponse);
            verify(userMapper).updateUser(eq(testUser), eq(request));
        }

        @Test
        @DisplayName("Update profile successfully: both fullName and username changed")
        void updateProfile_Success_UpdateBothFields() {
            // usernameChangedAt = null → canChangeUsername = true
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, "newuser", EMAIL, "New Name", null, Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);
            doAnswer(invocation -> {
                User u = invocation.getArgument(0);
                UpdateProfileRequest req = invocation.getArgument(1);
                if (req.fullName() != null)
                    u.setFullName(req.fullName());
                if (req.username() != null)
                    u.setUsername(req.username());
                return null;
            }).when(userMapper).updateUser(eq(testUser), any(UpdateProfileRequest.class));

            UpdateProfileRequest request = new UpdateProfileRequest(null, "New Name", "newuser");
            UserMeResponse result = userService.updateProfile(USER_ID, request);

            assertThat(testUser.getUsernameChangedAt()).isNotNull();
            assertThat(result).isEqualTo(expectedResponse);
            verify(userMapper).updateUser(eq(testUser), eq(request));
        }

        @Test
        @DisplayName("Update profile successfully: same username passed → no change, no cooldown trigger")
        void updateProfile_Success_SameUsernamePassed() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null, Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(null, FULL_NAME, USERNAME); // same username
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getUsernameChangedAt()).isNull();
            verify(userMapper).updateUser(eq(testUser), eq(request));
        }

        @Test
        @DisplayName("Update profile failed: username cooldown active")
        void updateProfile_Failure_UsernameCooldown() {
            testUser.setUsernameChangedAt(LocalDateTime.now().minusDays(5)); // within 30-day cooldown
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UpdateProfileRequest request = new UpdateProfileRequest(null, FULL_NAME, "newuser");

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.CANNOT_CHANGE_USERNAME_YET));
        }

        @Test
        @DisplayName("Update profile failed: username already exists")
        void updateProfile_Failure_UsernameAlreadyExists() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername("takenuser")).thenReturn(true);

            UpdateProfileRequest request = new UpdateProfileRequest(null, FULL_NAME, "takenuser");

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USERNAME_EXISTED));
        }

        @Test
        @DisplayName("Update profile failed: user not found")
        void updateProfile_Failure_UserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            UpdateProfileRequest request = new UpdateProfileRequest(null, "New Name", "newuser");

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }
    }

    // ========================================================================
    // updateProfile - avatar
    // ========================================================================
    @Nested
    @DisplayName("updateProfile - avatar")
    class UpdateProfileAvatarTests {

        @Test
        @DisplayName("Update profile successfully: set first avatar")
        void updateProfile_Success_SetFirstAvatar() {
            testUser.setAvatarUrl(null);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, NEW_UPLOADS_KEY,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(NEW_VERIFIED_KEY, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isEqualTo(NEW_UPLOADS_KEY);
            verify(publisher).publishEvent(eq(new FileAttachedEvent(NEW_VERIFIED_KEY, NEW_UPLOADS_KEY)));
            verify(publisher, never()).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Update profile successfully: replace avatar")
        void updateProfile_Success_ReplaceAvatar() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, NEW_UPLOADS_KEY,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(NEW_VERIFIED_KEY, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isEqualTo(NEW_UPLOADS_KEY);
            verify(publisher).publishEvent(eq(new FileAttachedEvent(NEW_VERIFIED_KEY, NEW_UPLOADS_KEY)));
            verify(publisher).publishEvent(eq(new FileDeletedEvent(List.of(OLD_UPLOADS_KEY))));
        }

        @Test
        @DisplayName("Update profile successfully: keep avatar when unchanged")
        void updateProfile_Success_KeepAvatarWhenUnchanged() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, OLD_UPLOADS_KEY,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(OLD_UPLOADS_KEY, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isEqualTo(OLD_UPLOADS_KEY);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile successfully: delete avatar")
        void updateProfile_Success_DeleteAvatar() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(null, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isNull();
            verify(publisher).publishEvent(eq(new FileDeletedEvent(List.of(OLD_UPLOADS_KEY))));
            verify(publisher, never()).publishEvent(any(FileAttachedEvent.class));
        }

        @Test
        @DisplayName("Update profile successfully: delete avatar when blank")
        void updateProfile_Success_DeleteAvatarWhenBlank() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest("   ", FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isNull();
            verify(publisher).publishEvent(eq(new FileDeletedEvent(List.of(OLD_UPLOADS_KEY))));
            verify(publisher, never()).publishEvent(any(FileAttachedEvent.class));
        }

        @Test
        @DisplayName("Update profile successfully: null when no avatar")
        void updateProfile_Success_NullWhenNoAvatar() {
            testUser.setAvatarUrl(null);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(null, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isNull();
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile successfully: retry same verified key")
        void updateProfile_Success_RetrySameVerifiedKey() {
            testUser.setAvatarUrl(NEW_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, NEW_UPLOADS_KEY,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(NEW_VERIFIED_KEY, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isEqualTo(NEW_UPLOADS_KEY);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile successfully: replace legacy non-uploads avatar")
        void updateProfile_Success_ReplaceLegacyNonUploadsAvatar() {
            String legacyUrl = "https://legacy.example.com/a.png";
            testUser.setAvatarUrl(legacyUrl);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, NEW_UPLOADS_KEY,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(NEW_VERIFIED_KEY, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isEqualTo(NEW_UPLOADS_KEY);
            verify(publisher).publishEvent(eq(new FileAttachedEvent(NEW_VERIFIED_KEY, NEW_UPLOADS_KEY)));
            verify(publisher, never()).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Update profile successfully: delete legacy non-uploads avatar")
        void updateProfile_Success_DeleteLegacyNonUploadsAvatar() {
            String legacyUrl = "https://legacy.example.com/a.png";
            testUser.setAvatarUrl(legacyUrl);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            UserMeResponse expectedResponse = new UserMeResponse(
                    USER_ID, USERNAME, EMAIL, FULL_NAME, null,
                    Role.USER, true, true, null, null, true, false);
            when(userMapper.toUserMeResponse(testUser)).thenReturn(expectedResponse);

            UpdateProfileRequest request = new UpdateProfileRequest(null, FULL_NAME, USERNAME);
            userService.updateProfile(USER_ID, request);

            assertThat(testUser.getAvatarUrl()).isNull();
            verify(publisher, never()).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Update profile failed: other user verified key")
        void updateProfile_Failure_OtherUserVerifiedKey() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UpdateProfileRequest request = new UpdateProfileRequest("verified/2/x.png", FULL_NAME, USERNAME);

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));

            assertThat(testUser.getAvatarUrl()).isEqualTo(OLD_UPLOADS_KEY);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile failed: other user uploads key")
        void updateProfile_Failure_OtherUserUploadsKey() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UpdateProfileRequest request = new UpdateProfileRequest("uploads/2/x.png", FULL_NAME, USERNAME);

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));

            assertThat(testUser.getAvatarUrl()).isEqualTo(OLD_UPLOADS_KEY);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile failed: own uploads key different from current")
        void updateProfile_Failure_OwnUploadsKeyDifferentFromCurrent() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UpdateProfileRequest request = new UpdateProfileRequest("uploads/1/other.png", FULL_NAME, USERNAME);

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));

            assertThat(testUser.getAvatarUrl()).isEqualTo(OLD_UPLOADS_KEY);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile failed: own staging key")
        void updateProfile_Failure_OwnStagingKey() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            UpdateProfileRequest request = new UpdateProfileRequest("staging/1/x.png", FULL_NAME, USERNAME);

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));

            assertThat(testUser.getAvatarUrl()).isEqualTo(OLD_UPLOADS_KEY);
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("Update profile failed: username taken with new avatar")
        void updateProfile_Failure_UsernameTakenWithNewAvatar() {
            testUser.setAvatarUrl(OLD_UPLOADS_KEY);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByUsername("takenuser")).thenReturn(true);

            UpdateProfileRequest request = new UpdateProfileRequest(NEW_VERIFIED_KEY, FULL_NAME, "takenuser");

            assertThatThrownBy(() -> userService.updateProfile(USER_ID, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USERNAME_EXISTED));

            assertThat(testUser.getAvatarUrl()).isEqualTo(OLD_UPLOADS_KEY);
            verifyNoInteractions(publisher);
            verify(userMapper, never()).updateUser(any(), any());
        }
    }

    // ========================================================================
    // User Statistics Queries Tests
    // ========================================================================
    @Nested
    @DisplayName("User Statistics Queries Tests")
    class UserStatsQueriesTests {

        private final LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        private final LocalDateTime to = LocalDateTime.of(2026, 11, 5, 23, 59, 59);

        @Test
        @DisplayName("countNewUsers should delegate to countNewUsersByDay for DAY granularity")
        void countNewUsers_Day_CallsRepository() {
            com.elingo.user.dto.projection.NewUserStats stat = org.mockito.Mockito.mock(com.elingo.user.dto.projection.NewUserStats.class);
            when(userRepository.countNewUsersByDay(from, to)).thenReturn(List.of(stat));

            List<com.elingo.user.dto.projection.NewUserStats> result = userService.countNewUsers(from, to, com.elingo.common.enums.StatsGranularity.DAY);

            assertThat(result).hasSize(1).contains(stat);
            verify(userRepository).countNewUsersByDay(from, to);
        }

        @Test
        @DisplayName("countNewUsers should delegate to countNewUsersByWeek for WEEK granularity")
        void countNewUsers_Week_CallsRepository() {
            com.elingo.user.dto.projection.NewUserStats stat = org.mockito.Mockito.mock(com.elingo.user.dto.projection.NewUserStats.class);
            when(userRepository.countNewUsersByWeek(from, to)).thenReturn(List.of(stat));

            List<com.elingo.user.dto.projection.NewUserStats> result = userService.countNewUsers(from, to, com.elingo.common.enums.StatsGranularity.WEEK);

            assertThat(result).hasSize(1).contains(stat);
            verify(userRepository).countNewUsersByWeek(from, to);
        }

        @Test
        @DisplayName("countNewUsers should delegate to countNewUsersByMonth for MONTH granularity")
        void countNewUsers_Month_CallsRepository() {
            com.elingo.user.dto.projection.NewUserStats stat = org.mockito.Mockito.mock(com.elingo.user.dto.projection.NewUserStats.class);
            when(userRepository.countNewUsersByMonth(from, to)).thenReturn(List.of(stat));

            List<com.elingo.user.dto.projection.NewUserStats> result = userService.countNewUsers(from, to, com.elingo.common.enums.StatsGranularity.MONTH);

            assertThat(result).hasSize(1).contains(stat);
            verify(userRepository).countNewUsersByMonth(from, to);
        }

        @Test
        @DisplayName("countTotalUsers should delegate to userRepository.count")
        void countTotalUsers_CallsRepository() {
            when(userRepository.count()).thenReturn(100L);

            assertThat(userService.countTotalUsers()).isEqualTo(100L);
            verify(userRepository).count();
        }

        @Test
        @DisplayName("countVerifiedUsers should delegate to userRepository.countByIsVerified")
        void countVerifiedUsers_CallsRepository() {
            when(userRepository.countByIsVerified(true)).thenReturn(80L);

            assertThat(userService.countVerifiedUsers()).isEqualTo(80L);
            verify(userRepository).countByIsVerified(true);
        }

        @Test
        @DisplayName("countActiveUsers should delegate to userRepository.countByIsActive")
        void countActiveUsers_CallsRepository() {
            when(userRepository.countByIsActive(true)).thenReturn(90L);

            assertThat(userService.countActiveUsers()).isEqualTo(90L);
            verify(userRepository).countByIsActive(true);
        }

        @Test
        @DisplayName("countUsersBefore should delegate to userRepository.countByCreatedAtBefore")
        void countUsersBefore_CallsRepository() {
            when(userRepository.countByCreatedAtBefore(from)).thenReturn(50L);

            assertThat(userService.countUsersBefore(from)).isEqualTo(50L);
            verify(userRepository).countByCreatedAtBefore(from);
        }
    }

    @Nested
    @DisplayName("Admin User Management Tests")
    class AdminUserManagementTests {

        private static final Long ADMIN_ID = 1L;
        private static final Long TARGET_USER_ID = 2L;

        private AdminUserResponse sampleAdminUserResponse(Long id, boolean active) {
            return new AdminUserResponse(
                    id,
                    "target_user",
                    "target@example.com",
                    "Target User",
                    "avatar.png",
                    Role.USER,
                    true,
                    active,
                    null,
                    null,
                    true,
                    false,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        }

        @Nested
        @DisplayName("searchUsersForAdmin Tests")
        class SearchUsersForAdminTests {

            @Test
            @DisplayName("Should query repository with specification and return PageResponse")
            @SuppressWarnings("unchecked")
            void searchUsersForAdmin_Success() {
                AdminUserSearchRequest request = new AdminUserSearchRequest(
                        "target",
                        Role.USER,
                        true,
                        true,
                        null,
                        null
                );
                Pageable pageable = PageRequest.of(0, 10);
                User user = User.builder()
                        .id(TARGET_USER_ID)
                        .username("target_user")
                        .email("target@example.com")
                        .fullName("Target User")
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                Page<User> page = new PageImpl<>(List.of(user), pageable, 1);
                AdminUserResponse userResponse = sampleAdminUserResponse(TARGET_USER_ID, true);

                when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
                when(userMapper.toAdminUserResponse(user)).thenReturn(userResponse);

                PageResponse<AdminUserResponse> result = userService.searchUsersForAdmin(request, pageable);

                assertThat(result).isNotNull();
                assertThat(result.content()).hasSize(1);
                assertThat(result.content().getFirst().username()).isEqualTo("target_user");
                assertThat(result.totalElements()).isEqualTo(1L);
                verify(userRepository).findAll(any(Specification.class), eq(pageable));
            }
        }

        @Nested
        @DisplayName("getById Tests")
        class GetByIdTests {

            @Test
            @DisplayName("Should return AdminUserResponse when user exists")
            void getById_WhenUserExists_ReturnsResponse() {
                User user = User.builder().id(TARGET_USER_ID).username("target_user").build();
                AdminUserResponse expected = sampleAdminUserResponse(TARGET_USER_ID, true);

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
                when(userMapper.toAdminUserResponse(user)).thenReturn(expected);

                AdminUserResponse result = userService.getById(TARGET_USER_ID);

                assertThat(result).isNotNull();
                assertThat(result.id()).isEqualTo(TARGET_USER_ID);
                verify(userRepository).findById(TARGET_USER_ID);
            }

            @Test
            @DisplayName("Should throw USER_NOT_FOUND when user does not exist")
            void getById_WhenUserNotFound_ThrowsException() {
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> userService.getById(999L))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            }
        }

        @Nested
        @DisplayName("lockUser Tests")
        class LockUserTests {

            @Test
            @DisplayName("Should lock user successfully, set inactive and update ban reason")
            void lockUser_Success() {
                User user = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.USER)
                        .isActive(true)
                        .build();
                AdminUserResponse expected = sampleAdminUserResponse(TARGET_USER_ID, false);

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));
                when(userMapper.toAdminUserResponse(user)).thenReturn(expected);

                AdminUserResponse result = userService.lockUser(ADMIN_ID, TARGET_USER_ID, "Violation of rules");

                assertThat(result).isNotNull();
                assertThat(user.getIsActive()).isFalse();
                assertThat(user.getBanReason()).isEqualTo("Violation of rules");
                verify(userRepository).findById(TARGET_USER_ID);
            }

            @Test
            @DisplayName("Should throw CANNOT_MODIFY_SELF when admin tries to lock self")
            void lockUser_WhenAdminLocksSelf_ThrowsCannotModifySelf() {
                assertThatThrownBy(() -> userService.lockUser(ADMIN_ID, ADMIN_ID, "Self lock reason"))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_SELF));
                verify(userRepository, never()).findById(any());
            }

            @Test
            @DisplayName("Should throw CANNOT_MODIFY_ADMIN when target user is an administrator")
            void lockUser_WhenTargetUserIsAdmin_ThrowsCannotModifyAdmin() {
                User adminTarget = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.ADMIN)
                        .isActive(true)
                        .build();

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(adminTarget));

                assertThatThrownBy(() -> userService.lockUser(ADMIN_ID, TARGET_USER_ID, "Lock reason"))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_ADMIN));
            }

            @Test
            @DisplayName("Should throw USER_INACTIVE when target user is already inactive")
            void lockUser_WhenUserAlreadyInactive_ThrowsUserInactive() {
                User inactiveUser = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.USER)
                        .isActive(false)
                        .build();

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(inactiveUser));

                assertThatThrownBy(() -> userService.lockUser(ADMIN_ID, TARGET_USER_ID, "Lock reason"))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_INACTIVE));
            }

            @Test
            @DisplayName("Should throw INVALID_REQUEST when lock reason is blank or null")
            void lockUser_WhenReasonIsBlank_ThrowsInvalidRequest() {
                User activeUser = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.USER)
                        .isActive(true)
                        .build();

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(activeUser));

                assertThatThrownBy(() -> userService.lockUser(ADMIN_ID, TARGET_USER_ID, "   "))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.INVALID_REQUEST));
            }

            @Test
            @DisplayName("Should throw USER_NOT_FOUND when target user does not exist")
            void lockUser_WhenUserNotFound_ThrowsUserNotFound() {
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> userService.lockUser(ADMIN_ID, 999L, "Lock reason"))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            }
        }

        @Nested
        @DisplayName("unlockUser Tests")
        class UnlockUserTests {

            @Test
            @DisplayName("Should unlock user successfully, set active and clear ban reason")
            void unlockUser_Success() {
                User lockedUser = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.USER)
                        .isActive(false)
                        .banReason("Old reason")
                        .build();
                AdminUserResponse expected = sampleAdminUserResponse(TARGET_USER_ID, true);

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(lockedUser));
                when(userMapper.toAdminUserResponse(lockedUser)).thenReturn(expected);

                AdminUserResponse result = userService.unlockUser(ADMIN_ID, TARGET_USER_ID);

                assertThat(result).isNotNull();
                assertThat(lockedUser.getIsActive()).isTrue();
                assertThat(lockedUser.getBanReason()).isNull();
                verify(userRepository).findById(TARGET_USER_ID);
            }

            @Test
            @DisplayName("Should throw CANNOT_MODIFY_SELF when admin tries to unlock self")
            void unlockUser_WhenAdminUnlocksSelf_ThrowsCannotModifySelf() {
                assertThatThrownBy(() -> userService.unlockUser(ADMIN_ID, ADMIN_ID))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_SELF));
                verify(userRepository, never()).findById(any());
            }

            @Test
            @DisplayName("Should throw CANNOT_MODIFY_ADMIN when target user is an administrator")
            void unlockUser_WhenTargetUserIsAdmin_ThrowsCannotModifyAdmin() {
                User adminTarget = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.ADMIN)
                        .isActive(false)
                        .build();

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(adminTarget));

                assertThatThrownBy(() -> userService.unlockUser(ADMIN_ID, TARGET_USER_ID))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.CANNOT_MODIFY_ADMIN));
            }

            @Test
            @DisplayName("Should throw USER_ALREADY_ACTIVE when target user is already active")
            void unlockUser_WhenUserAlreadyActive_ThrowsUserAlreadyActive() {
                User activeUser = User.builder()
                        .id(TARGET_USER_ID)
                        .role(Role.USER)
                        .isActive(true)
                        .build();

                when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(activeUser));

                assertThatThrownBy(() -> userService.unlockUser(ADMIN_ID, TARGET_USER_ID))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_ALREADY_ACTIVE));
            }

            @Test
            @DisplayName("Should throw USER_NOT_FOUND when target user does not exist")
            void unlockUser_WhenUserNotFound_ThrowsUserNotFound() {
                when(userRepository.findById(999L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> userService.unlockUser(ADMIN_ID, 999L))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getAppError()).isEqualTo(AppError.USER_NOT_FOUND));
            }
        }
    }
}
