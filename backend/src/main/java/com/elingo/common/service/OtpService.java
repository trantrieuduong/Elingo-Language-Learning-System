package com.elingo.common.service;

import com.elingo.common.util.OtpType;

public interface OtpService {
    String generateAndSaveOtp(OtpType type, String identifier);
    void verifyOtp(OtpType type, String identifier, String inputOtp);
}
