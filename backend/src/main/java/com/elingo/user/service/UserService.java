package com.elingo.user.service;

import com.elingo.user.dto.request.ChangePasswordRequest;

public interface UserService {
    void changePassword(String username, ChangePasswordRequest request);
}