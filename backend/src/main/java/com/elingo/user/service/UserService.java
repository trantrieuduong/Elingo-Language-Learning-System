package com.elingo.user.service;

import com.elingo.user.dto.request.AdminUserSearchRequest;
import com.elingo.user.dto.response.AdminUserResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.StatsGranularity;
import com.elingo.user.dto.projection.NewUserStats;
import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.request.UpdateProfileRequest;
import com.elingo.user.dto.response.UserMeResponse;
import com.elingo.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;

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
    List<NewUserStats> countNewUsers(LocalDateTime from, LocalDateTime to, StatsGranularity granularity);
    long countTotalUsers();
    long countVerifiedUsers();
    long countActiveUsers();
    long countUsersBefore(LocalDateTime before);

    // Admin user management
    PageResponse<AdminUserResponse> searchUsersForAdmin(AdminUserSearchRequest request, Pageable pageable);
    AdminUserResponse getById(Long userId);
    AdminUserResponse lockUser(Long adminId, Long userId, String reason);
    AdminUserResponse unlockUser(Long adminId, Long userId);
}
