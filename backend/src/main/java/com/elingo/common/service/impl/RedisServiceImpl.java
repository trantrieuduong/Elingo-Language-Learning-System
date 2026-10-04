package com.elingo.common.service.impl;

import com.elingo.common.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>("""
        local c = redis.call('INCR', KEYS[1])
        if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
        return c
        """, Long.class); // INCR và PEXPIRE chạy trong một bước

    @Override
    public void save(String key, String value, long timeoutInMinutes) {
        stringRedisTemplate.opsForValue().set(key, value, timeoutInMinutes, TimeUnit.MINUTES);
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public long incrementWithTtl(String key, Duration ttl) {
        return stringRedisTemplate.execute(
                INCR_WITH_TTL, List.of(key), String.valueOf(ttl.toMillis()));
    }

    @Override
    public long getTtlSeconds(String key) {
        Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 0 ? 1 : ttl;
    }
}