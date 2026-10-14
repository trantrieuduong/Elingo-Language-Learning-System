package com.elingo.gamification.listener;

import com.elingo.common.event.XpEvent;
import com.elingo.gamification.config.GamificationProperties;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.service.CheckInService;
import com.elingo.gamification.service.XpPolicy;
import com.elingo.gamification.service.XpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LearningXpListenerTest {

    @Mock
    private CheckInService checkInService;

    @Mock
    private XpService xpService;

    @Mock
    private XpPolicy xpPolicy;

    @Mock
    private GamificationProperties props;

    @InjectMocks
    private LearningXpListener listener;

    private static final Long USER_ID = 10L;
    private static final Long REF_ID = 99L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 10, 14, 9, 30);

    @Nested
    @DisplayName("on")
    class OnTests {

        @Test
        @DisplayName("Process experience points event successfully with allowed policy")
        void on_Success_AllowedPolicy() {
            GamificationProperties.Rule rule = new GamificationProperties.Rule(30, 5);
            XpEvent event = new XpEvent(USER_ID, REF_ID, XpSource.LESSON_DICTATION, OCCURRED_AT);

            when(props.rule(XpSource.LESSON_DICTATION)).thenReturn(rule);
            when(xpPolicy.allow(USER_ID, XpSource.LESSON_DICTATION, OCCURRED_AT.toLocalDate())).thenReturn(true);

            listener.on(event);

            verify(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);
            verify(xpPolicy).allow(USER_ID, XpSource.LESSON_DICTATION, OCCURRED_AT.toLocalDate());
            verify(xpService).addXp(USER_ID, 30, XpSource.LESSON_DICTATION, REF_ID);
        }

        @Test
        @DisplayName("Skip processing when rule is null")
        void on_Skip_RuleIsNull() {
            XpEvent event = new XpEvent(USER_ID, REF_ID, XpSource.BATTLE_WIN, OCCURRED_AT);

            when(props.rule(XpSource.BATTLE_WIN)).thenReturn(null);

            listener.on(event);

            verify(checkInService, never()).ensureCheckedIn(anyLong(), any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
        }

        @Test
        @DisplayName("Skip processing when source is daily check in")
        void on_Skip_DailyCheckIn() {
            GamificationProperties.Rule rule = new GamificationProperties.Rule(10, 5);
            XpEvent event = new XpEvent(USER_ID, REF_ID, XpSource.DAILY_CHECKIN, OCCURRED_AT);

            when(props.rule(XpSource.DAILY_CHECKIN)).thenReturn(rule);

            listener.on(event);

            verify(checkInService, never()).ensureCheckedIn(anyLong(), any());
            verify(xpPolicy, never()).allow(anyLong(), any(), any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
        }

        @Test
        @DisplayName("Skip adding experience when policy denies")
        void on_Skip_PolicyDenied() {
            GamificationProperties.Rule rule = new GamificationProperties.Rule(25, 5);
            XpEvent event = new XpEvent(USER_ID, REF_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT);

            when(props.rule(XpSource.FLASHCARD_REVIEW)).thenReturn(rule);
            when(xpPolicy.allow(USER_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT.toLocalDate())).thenReturn(false);

            listener.on(event);

            verify(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);
            verify(xpPolicy).allow(USER_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT.toLocalDate());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
        }

        @Test
        @DisplayName("Process successfully when check in service throws exception then recovers")
        void on_Success_CheckInExceptionRecovered() {
            GamificationProperties.Rule rule = new GamificationProperties.Rule(40, 5);
            XpEvent event = new XpEvent(USER_ID, REF_ID, XpSource.LESSON_SHADOWING, OCCURRED_AT);

            when(props.rule(XpSource.LESSON_SHADOWING)).thenReturn(rule);
            doThrow(new RuntimeException("Redis down")).when(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);

            listener.on(event);

            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
        }
    }
}
