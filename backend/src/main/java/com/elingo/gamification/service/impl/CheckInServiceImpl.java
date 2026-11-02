package com.elingo.gamification.service.impl;

import com.elingo.common.service.RedisService;
import com.elingo.gamification.service.CheckInRecorder;
import com.elingo.gamification.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CHECKIN-SERVICE")
public class CheckInServiceImpl implements CheckInService {

    private final RedisService redis;
    private final CheckInRecorder recorder;

    @Override
    public void ensureCheckedIn(long userId, LocalDateTime at) {
        LocalDate date = at.toLocalDate();
        String key = "checkin:" + date;
        boolean bitSetByMe = false;

        try {
            if (redis.getBit(key, userId)) return;
            if (redis.setBit(key, userId, true)) return;
            bitSetByMe = true;
            redis.expireAt(key, date.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, falling back to DB. userId={}", userId, e);
        }

        try {
            recorder.record(userId, date);
        } catch (RuntimeException e) {
            if (bitSetByMe) clearBit(key, userId);
            throw e;
        }
    }

    private void clearBit(String key, long userId) {
        try {
            redis.setBit(key, userId, false);
        }
        catch (RuntimeException e) {
            log.warn("Clear bit failed bit={} userId={}", key, userId, e);
        }
    }
}
