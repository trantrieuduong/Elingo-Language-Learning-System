package com.elingo.common.service;

import com.elingo.common.enums.OtpType;

public interface OtpService {
    String generateAndSaveOtp(OtpType type, String identifier);
    void verifyOtp(OtpType type, String identifier, String inputOtp);
    void checkOtp(OtpType type, String identifier, String inputOtp);
    void consumeOtp(OtpType type, String identifier);
}
