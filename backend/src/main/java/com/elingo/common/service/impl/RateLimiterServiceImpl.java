package com.elingo.common.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.service.RateLimiterService;
import com.elingo.common.service.RedisService;
import com.elingo.common.enums.RateLimitRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final RedisService redisService;

    @Override
    public void check(RateLimitRule rule, String subject) {
        String key = rule.key(subject);
        long count = redisService.incrementWithTtl(key, rule.window());
        if (count > rule.max())
            throw new AppException(AppError.RATE_LIMIT_EXCEEDED);
    }
}
