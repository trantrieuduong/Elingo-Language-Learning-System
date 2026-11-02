package com.elingo.common.service;

import com.elingo.common.enums.RateLimitRule;

public interface RateLimiterService {
    void check(RateLimitRule rule, String subject);
}
