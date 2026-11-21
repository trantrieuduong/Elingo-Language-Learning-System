package com.elingo.gamification.listener;

import com.elingo.common.event.XpGainedEvent;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.publisher.GamificationEventPublisher;
import com.elingo.gamification.service.CheckInService;
import com.elingo.gamification.service.XpPolicy;
import com.elingo.gamification.service.XpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("LearningXpListener Tests")
class LearningXpListenerTest {

    @Mock
    private CheckInService checkInService;

    @Mock
    private XpService xpService;

    @Mock
    private XpPolicy xpPolicy;

    @Mock
    private GamificationEventPublisher gamificationEventPublisher;

    private LearningXpListener listener;

    private static final Long USER_ID = 10L;
    private static final Long REF_ID = 99L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 10, 14, 9, 30);

    @BeforeEach
    void setUp() {
        listener = new LearningXpListener(
                checkInService,
                xpService,
                xpPolicy,
                gamificationEventPublisher
        );
    }

    @Nested
    @DisplayName("on Event Handling Tests")
    class OnEventTests {

        @Test
        @DisplayName("Should successfully grant XP when policy allows")
        void on_Success_WhenPolicyAllows() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.SEGMENT_DICTATION, OCCURRED_AT);

            when(xpPolicy.getRule(XpSource.SEGMENT_DICTATION)).thenReturn(new XpPolicy.Rule(30, 5));
            when(xpPolicy.allow(USER_ID, XpSource.SEGMENT_DICTATION, OCCURRED_AT.toLocalDate())).thenReturn(true);

            listener.on(event);

            verify(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);
            verify(xpPolicy).allow(USER_ID, XpSource.SEGMENT_DICTATION, OCCURRED_AT.toLocalDate());
            verify(xpService).addXp(USER_ID, 30, XpSource.SEGMENT_DICTATION, REF_ID);
            verify(gamificationEventPublisher).sendXpGained(USER_ID, 30);
        }

        @Test
        @DisplayName("Should skip processing when rule is not found")
        void on_Failure_WhenRuleNotFound() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.BATTLE_WIN, OCCURRED_AT);

            when(xpPolicy.getRule(XpSource.BATTLE_WIN)).thenReturn(null);

            listener.on(event);

            verify(checkInService, never()).ensureCheckedIn(anyLong(), any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Should skip processing when source is DAILY_CHECKIN")
        void on_Skip_WhenDailyCheckInSource() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.DAILY_CHECKIN, OCCURRED_AT);

            when(xpPolicy.getRule(XpSource.DAILY_CHECKIN)).thenReturn(new XpPolicy.Rule(10, 1));

            listener.on(event);

            verify(checkInService, never()).ensureCheckedIn(anyLong(), any());
            verify(xpPolicy, never()).allow(anyLong(), any(), any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Should not add XP when policy limit is exceeded")
        void on_Failure_WhenPolicyLimitExceeded() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT);

            when(xpPolicy.getRule(XpSource.FLASHCARD_REVIEW)).thenReturn(new XpPolicy.Rule(25, 5));
            when(xpPolicy.allow(USER_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT.toLocalDate())).thenReturn(false);

            listener.on(event);

            verify(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);
            verify(xpPolicy).allow(USER_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT.toLocalDate());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Should catch exception and log error without throwing when check-in fails")
        void on_Failure_WhenCheckInServiceFails() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.SEGMENT_SHADOWING, OCCURRED_AT);

            when(xpPolicy.getRule(XpSource.SEGMENT_SHADOWING)).thenReturn(new XpPolicy.Rule(40, 5));
            doThrow(new RuntimeException("Database error")).when(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);

            listener.on(event);

            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }
    }
}
