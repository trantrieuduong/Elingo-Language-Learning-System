package com.elingo.common.service;

import java.time.Duration;
import java.time.Instant;

public interface RedisService {
    void save(String key, String value, long timeoutInMinutes);
    String get(String key);
    void delete(String key);
    boolean exists(String key);

    /** Tăng bộ đếm, đặt TTL nếu là lần đầu. Trả về giá trị sau khi tăng. */
    long incrementWithTtl(String key, Duration ttl);
    long getTtlSeconds(String key);

    boolean getBit(String key, long offset);

    /** Đặt bit và trả về giá trị bit TRƯỚC khi đặt (true = đã được set từ trước). */
    boolean setBit(String key, long offset, boolean value);

    void expireAt(String key, Instant at);
}
