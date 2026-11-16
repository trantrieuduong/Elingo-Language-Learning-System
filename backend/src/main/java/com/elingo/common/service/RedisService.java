package com.elingo.common.service;

import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

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
    void expire(String key, Duration duration);

    Boolean zAdd(String key, String value, double score);
    Double zIncrBy(String key, String value, double delta);
    Double zScore(String key, String value);
    Long zReverseRank(String key, String value);
    Long zCard(String key);
    Set<ZSetOperations.TypedTuple<String>> zReverseRangeWithScores(String key, long start, long end);
}
