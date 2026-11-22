package com.elingo.gamification.service;

import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.service.RedisService;
import com.elingo.common.service.SystemConfigService;
import com.elingo.gamification.entity.XpSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("XpPolicy Tests")
class XpPolicyTest {

    @Mock
    private RedisService redis;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private XpPolicy xpPolicy;

    private static final long USER_ID = 100L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 21);

    @Nested
    @DisplayName("allow tests")
    class AllowTests {

        @Test
        @DisplayName("allow returns true when under daily limit")
        void allow_UnderLimit_ReturnsTrue() {
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_XP), anyInt()))
                    .thenReturn(2);
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_DAILY_LIMIT), anyInt()))
                    .thenReturn(100);
            when(redis.incrementWithTtl(anyString(), any(Duration.class))).thenReturn(10L);

            boolean allowed = xpPolicy.allow(USER_ID, XpSource.FLASHCARD_REVIEW, DATE);

            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("allow returns false when exceeds daily limit")
        void allow_ExceedsLimit_ReturnsFalse() {
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_XP), anyInt()))
                    .thenReturn(2);
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_DAILY_LIMIT), anyInt()))
                    .thenReturn(100);
            when(redis.incrementWithTtl(anyString(), any(Duration.class))).thenReturn(101L);

            boolean allowed = xpPolicy.allow(USER_ID, XpSource.FLASHCARD_REVIEW, DATE);

            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("allow returns true when Redis throws exception (graceful degradation)")
        void allow_RedisFails_ReturnsTrue() {
            when(systemConfigService.getInt(any(SystemConfigKey.class), anyInt())).thenReturn(10);
            when(redis.incrementWithTtl(anyString(), any(Duration.class)))
                    .thenThrow(new RuntimeException("Redis unavailable"));

            boolean allowed = xpPolicy.allow(USER_ID, XpSource.BATTLE_WIN, DATE);

            assertThat(allowed).isTrue();
        }
    }

    @Nested
    @DisplayName("getRule & getXp & getDailyLimit tests")
    class QueryRuleTests {

        @Test
        @DisplayName("getRule returns configured xp and daily limit")
        void getRule_Success() {
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_XP), eq(2)))
                    .thenReturn(5);
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_FLASHCARD_REVIEW_DAILY_LIMIT), eq(100)))
                    .thenReturn(200);

            XpPolicy.Rule rule = xpPolicy.getRule(XpSource.FLASHCARD_REVIEW);

            assertThat(rule).isNotNull();
            assertThat(rule.xp()).isEqualTo(5);
            assertThat(rule.dailyLimit()).isEqualTo(200);
        }

        @Test
        @DisplayName("getXp and getDailyLimit delegate to getRule")
        void getXpAndLimit_Success() {
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_DAILY_CHECKIN_XP), eq(5)))
                    .thenReturn(15);
            when(systemConfigService.getInt(eq(SystemConfigKey.GAMIFICATION_RULE_DAILY_CHECKIN_DAILY_LIMIT), eq(1)))
                    .thenReturn(1);

            assertThat(xpPolicy.getXp(XpSource.DAILY_CHECKIN)).isEqualTo(15);
            assertThat(xpPolicy.getDailyLimit(XpSource.DAILY_CHECKIN)).isEqualTo(1);
        }

        @Test
        @DisplayName("getAllRules returns rules for all 6 sources")
        void getAllRules_Success() {
            when(systemConfigService.getInt(any(SystemConfigKey.class), anyInt())).thenReturn(10);

            Map<XpSource, XpPolicy.Rule> rules = xpPolicy.getAllRules();

            assertThat(rules).hasSize(6);
            assertThat(rules).containsKeys(
                    XpSource.FLASHCARD_REVIEW,
                    XpSource.SEGMENT_DICTATION,
                    XpSource.SEGMENT_SHADOWING,
                    XpSource.BATTLE_WIN,
                    XpSource.BATTLE_LOSE,
                    XpSource.DAILY_CHECKIN
            );
        }
    }
}
