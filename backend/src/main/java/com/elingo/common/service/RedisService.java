package com.elingo.common.service;

import java.time.Duration;

public interface RedisService {
    void save(String key, String value, long timeoutInMinutes);
    String get(String key);
    void delete(String key);
    boolean exists(String key);

    /** Tăng bộ đếm, đặt TTL nếu là lần đầu. Trả về giá trị sau khi tăng. */
    long incrementWithTtl(String key, Duration ttl);
    long getTtlSeconds(String key);
}
