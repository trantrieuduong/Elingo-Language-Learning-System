package com.elingo.user.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.common.service.EmailService;
import com.elingo.common.service.OtpService;
import com.elingo.common.enums.EmailTemplateName;
import com.elingo.common.enums.OtpType;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.user.service.UserService;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.file.util.FileKey;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher publisher;
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new AppException(AppError.OLD_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new AppException(AppError.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());

        log.info("Password changed userId={}", user.getId());
    }

    @Override
    @Transactional
    public void setPassword(Long userId, SetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (user.getPasswordHash() != null) {
            throw new AppException(AppError.PASSWORD_ALREADY_SET);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());

        log.info("Password set userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendOTPUpdateEmail(Long userId, SendOTPUpdateEmailRequest request) {
        String newEmail = request.newEmail();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        String oldEmail = user.getEmail();
        if (oldEmail.equals(newEmail)) {
            throw new AppException(AppError.EMAIL_UNCHANGED);
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(AppError.EMAIL_EXISTED);
        }

        String newEmailOtp = otpService.generateAndSaveOtp(OtpType.CHANGE_EMAIL, newEmail);
        emailService.sendEmail(
                newEmail,
                user.getUsername(),
                EmailTemplateName.SEND_OTP,
                newEmailOtp,
                OtpType.CHANGE_EMAIL.getTitle());
        log.info("Update email OTP sent email={} userId={}", newEmail, userId);
    }

    @Override
    @Transactional
    public void updateEmail(Long userId, UpdateEmailRequest request) {
        String newEmail = request.newEmail();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(AppError.EMAIL_EXISTED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(AppError.PASSWORD_INCORRECT);
        }

        otpService.verifyOtp(OtpType.CHANGE_EMAIL, newEmail, request.newEmailOtp());

        user.setGoogleProviderId(null);
        user.setEmail(newEmail);
        log.info("Email updated email={} userId={}", newEmail, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        return userMapper.toUserMeResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getUserById(Long targetUserId, Long currentUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (Objects.equals(targetUserId, currentUserId))
            return userMapper.toUserMeResponse(user);

        return userMapper.toUserPublicResponse(user);
    }

    @Override
    @Transactional
    public UserMeResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        // 1. Avatar: chỉ kiểm tra, chưa ghi 
        // null/rỗng = xoá; trùng key hiện tại = giữ; còn lại = ảnh mới.
        String newKey = StringUtils.hasText(request.avatarUrl()) ? request.avatarUrl() : null;
        String oldKey = user.getAvatarUrl();
        boolean avatarChanged = !Objects.equals(newKey, oldKey);
        String newUploadsKey = null;
        if (avatarChanged && newKey != null) {
            requireOwned(newKey, userId);                 // key phải thuộc chính user này
            newUploadsKey = FileKey.toUploads(newKey);    // sai vùng -> AppException(INVALID_REQUEST)
            if (newUploadsKey.equals(oldKey)) {           // gửi lại verified key đã dùng: bỏ qua hết
                avatarChanged = false;
            }
        }

        // 2. Username
        boolean isUsernameChanged = !request.username().equals(user.getUsername());
        if (isUsernameChanged) {
            if (!user.canChangeUsername()) {
                log.warn("Username change rejected due to cooldown userId={} lastChangedAt={}",
                        userId, user.getUsernameChangedAt());
                throw new AppException(AppError.CANNOT_CHANGE_USERNAME_YET);
            }
            if (userRepository.existsByUsername(request.username())) {
                log.warn("Username change rejected, already exists userId={}", userId);
                throw new AppException(AppError.USERNAME_EXISTED);
            }
        }

        // 3. Ghi các field thường
        userMapper.updateUser(user, request);
        if (isUsernameChanged) {
            user.setUsernameChangedAt(LocalDateTime.now());
        }

        // 4. Avatar: ghi key uploads/ (hoặc null) rồi phát event
        if (avatarChanged) {
            user.setAvatarUrl(newUploadsKey);
            if (newUploadsKey != null) {
                publisher.publishEvent(new FileAttachedEvent(newKey, newUploadsKey));
            }
            if (FileKey.isInZone(oldKey, FileKey.UPLOADS_PREFIX)) { // false khi oldKey null hoặc không phải file hệ thống
                publisher.publishEvent(new FileDeletedEvent(List.of(oldKey)));
            }
        }
        return userMapper.toUserMeResponse(user);
    }

    // Key phải thuộc chính user này
    private void requireOwned(String key, Long userId) {
        if (!FileKey.isOwnedBy(key, userId)) {
            throw new AppException(AppError.FILE_ACCESS_DENIED);
        }
    }
}
