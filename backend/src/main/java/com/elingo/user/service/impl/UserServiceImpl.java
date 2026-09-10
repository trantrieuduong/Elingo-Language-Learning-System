package com.elingo.user.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import com.elingo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        // 1. Kiểm tra mật khẩu cũ đúng không trước
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new AppException(AppError.OLD_PASSWORD_INCORRECT);
        }

        // 2. Mật khẩu cũ đúng rồi mới kiểm tra mật khẩu mới có đủ mạnh không
        if (request.newPassword() == null || !PASSWORD_PATTERN.matcher(request.newPassword()).matches()) {
            throw new AppException(AppError.PASSWORD_INVALID);
        }

        // 3. Kiểm tra mật khẩu mới có trùng mật khẩu cũ không
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new AppException(AppError.NEW_PASSWORD_SAME_AS_OLD);
        }

        // 4. Cập nhật mật khẩu mới (field tên là passwordHash)
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}