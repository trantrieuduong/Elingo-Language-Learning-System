package com.elingo.user.service;

import com.elingo.common.enums.StatsGranularity;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {
    void changePassword(Long userId, ChangePasswordRequest request);
    void setPassword(Long userId, SetPasswordRequest request);
    void sendOTPUpdateEmail(Long userId, SendOTPUpdateEmailRequest request);
    void updateEmail(Long userId, UpdateEmailRequest request);
    UserMeResponse getMyInfo(Long userId);
    Object getUserById(Long targetUserId, Long currentUserId);
    UserMeResponse updateProfile(Long userId, UpdateProfileRequest request);

    // User Stats for Admin
    List<UserRepository.NewUserStats> countNewUsers(LocalDateTime from, LocalDateTime to, StatsGranularity granularity);
    long countTotalUsers();
    long countVerifiedUsers();
    long countActiveUsers();
    long countUsersBefore(LocalDateTime before);
}
