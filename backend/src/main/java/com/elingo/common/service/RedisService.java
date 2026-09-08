package com.elingo.common.service;

public interface RedisService {
    void save(String key, String value, long timeoutInSeconds);
    String get(String key);
    void delete(String key);
    boolean exists(String key);
}
