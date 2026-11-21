package com.elingo.gamification.service;

import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.service.RedisService;
import com.elingo.common.service.SystemConfigService;
import com.elingo.gamification.entity.XpSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "XP-POLICY")
public class XpPolicy {

    private final RedisService redis;
    private final SystemConfigService systemConfigService;

    public record Rule(int xp, int dailyLimit) {}

    private record KeyPair(SystemConfigKey xpKey, SystemConfigKey dailyLimitKey, int defaultXp, int defaultDailyLimit) {}

    private static final Map<XpSource, KeyPair> SOURCE_CONFIG_MAP = Map.of(
            XpSource.FLASHCARD_REVIEW, new KeyPair(
                    SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_XP,
                    SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_DAILY_LIMIT,
                    2, 100),
            XpSource.SEGMENT_DICTATION, new KeyPair(
                    SystemConfigKey.GAMIFICATION_RULE_SEGMENT_DICTATION_XP,
                    SystemConfigKey.GAMIFICATION_RULE_SEGMENT_DICTATION_DAILY_LIMIT,
                    3, 50),
            XpSource.SEGMENT_SHADOWING, new KeyPair(
                    SystemConfigKey.GAMIFICATION_RULE_SEGMENT_SHADOWING_XP,
                    SystemConfigKey.GAMIFICATION_RULE_SEGMENT_SHADOWING_DAILY_LIMIT,
                    3, 50),
            XpSource.BATTLE_WIN, new KeyPair(
                    SystemConfigKey.GAMIFICATION_RULE_BATTLE_WIN_XP,
                    SystemConfigKey.GAMIFICATION_RULE_BATTLE_WIN_DAILY_LIMIT,
                    30, 10),
            XpSource.BATTLE_LOSE, new KeyPair(
                    SystemConfigKey.GAMIFICATION_RULE_BATTLE_LOSE_XP,
                    SystemConfigKey.GAMIFICATION_RULE_BATTLE_LOSE_DAILY_LIMIT,
                    10, 10),
            XpSource.DAILY_CHECKIN, new KeyPair(
                    SystemConfigKey.GAMIFICATION_RULE_DAILY_CHECKIN_XP,
                    SystemConfigKey.GAMIFICATION_RULE_DAILY_CHECKIN_DAILY_LIMIT,
                    5, 1)
    );

    public boolean allow(long userId, XpSource source, LocalDate date) {
        int limit = getDailyLimit(source);

        try {
            String key = "xp:cap:%d:%s:%s".formatted(userId, source, date);
            return redis.incrementWithTtl(key, Duration.ofDays(2)) <= limit;
        } catch (RuntimeException e) {
            log.warn("Daily xp cap check failed source={} date={}", source, date, e);
            return true;
        }
    }

    public Rule getRule(XpSource source) {
        if (source == null) return null;
        KeyPair pair = SOURCE_CONFIG_MAP.get(source);
        if (pair == null) return null;

        int xp = systemConfigService.getInt(pair.xpKey(), pair.defaultXp());
        int dailyLimit = systemConfigService.getInt(pair.dailyLimitKey(), pair.defaultDailyLimit());
        return new Rule(xp, dailyLimit);
    }

    public int getXp(XpSource source) {
        Rule rule = getRule(source);
        return rule != null ? rule.xp() : 0;
    }

    public int getDailyLimit(XpSource source) {
        Rule rule = getRule(source);
        return rule != null ? rule.dailyLimit() : 0;
    }

    public Map<XpSource, Rule> getAllRules() {
        Map<XpSource, Rule> result = new EnumMap<>(XpSource.class);
        for (XpSource source : SOURCE_CONFIG_MAP.keySet()) {
            result.put(source, getRule(source));
        }
        return result;
    }
}
