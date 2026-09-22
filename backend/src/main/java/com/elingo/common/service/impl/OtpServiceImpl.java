package com.elingo.common.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.OtpService;
import com.elingo.common.service.RedisService;
import com.elingo.common.util.OtpType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final RedisService redisService;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    @Override
    public String generateAndSaveOtp(OtpType type, String identifier) {
        String otp = String.format("%06d", RANDOM.nextInt(999999));
        String key = buildOtpKey(type, identifier);

        redisService.save(key, otp, otpExpirationMinutes);
        return otp;
    }

    @Override
    public void verifyOtp(OtpType type, String identifier, String inputOtp) {
        String key = buildOtpKey(type, identifier);

        String savedOtp = redisService.get(key);

        if (savedOtp == null || !Objects.equals(savedOtp, inputOtp))
            throw new AppException(AppError.OTP_INVALID);

        redisService.delete(key);
    }

    private String buildOtpKey(OtpType type, String identifier) {
        return "otp:" + type.getPrefix() + ":" + identifier;
    }
}
