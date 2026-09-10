package com.elingo.user.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Processing change password request for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            log.warn("Change password failed: Incorrect old password for userId={}", userId);
            throw new AppException(AppError.OLD_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            log.warn("Change password failed: New password is same as old password for userId={}", userId);
            throw new AppException(AppError.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());

        log.info("Password changed successfully for userId={}", user.getId());
    }
}