package com.elingo.gamification.service;

import com.elingo.common.service.RedisService;
import com.elingo.gamification.config.GamificationProperties;
import com.elingo.gamification.entity.XpSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "XP-POLICY")
public class XpPolicy {

    private final RedisService redis;
    private final GamificationProperties props;

    public boolean allow(long userId, XpSource source, LocalDate date) {
        int limit = props.rule(source).dailyLimit();

        try {
            String key = "xp:cap:%d:%s:%s".formatted(userId, source, date);
            return redis.incrementWithTtl(key, Duration.ofDays(2)) <= limit;
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, skip daily cap", e);
            return true;
        }
    }
}
