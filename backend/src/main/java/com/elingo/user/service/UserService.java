package com.elingo.user.service;

import com.elingo.user.dto.request.SendOTPUpdateEmailRequest;
import com.elingo.user.dto.request.UpdateEmailRequest;

public interface UserService {
    void sendOTPUpdateEmail(String username, SendOTPUpdateEmailRequest request);
    void updateEmail(String username, UpdateEmailRequest request);
}
