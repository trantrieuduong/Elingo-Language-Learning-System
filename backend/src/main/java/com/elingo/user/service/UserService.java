package com.elingo.user.service;

import com.elingo.user.dto.request.ChangePasswordRequest;
import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.SetPasswordRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;
import com.elingo.user.dto.response.UserMeResponse;

public interface UserService {
    void changePassword(Long userId, ChangePasswordRequest request);
    void setPassword(Long userId, SetPasswordRequest request);
    void sendOTPUpdateEmail(Long userId, SendOTPUpdateEmailRequest request);
    void updateEmail(Long userId, UpdateEmailRequest request);
    UserMeResponse getMyInfo(Long userId);
    Object getUserById(Long targetUserId, Long currentUserId);
}
