package com.elingo.gamification.listener;

import com.elingo.common.event.XpGainedEvent;
import com.elingo.gamification.config.GamificationProperties;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.service.CheckInService;
import com.elingo.gamification.service.XpPolicy;
import com.elingo.gamification.service.XpService;
import com.elingo.gamification.publisher.GamificationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

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
    private GamificationEventPublisher gamificationEventPublisher;

    private LearningXpListener listener;
    private GamificationProperties props;

    private static final Long USER_ID = 10L;
    private static final Long REF_ID = 99L;
    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 10, 14, 9, 30);

    @BeforeEach
    void setUp() {
        props = new GamificationProperties(Map.of(
                XpSource.SEGMENT_DICTATION, new GamificationProperties.Rule(30, 5),
                XpSource.DAILY_CHECKIN, new GamificationProperties.Rule(10, 5),
                XpSource.FLASHCARD_REVIEW, new GamificationProperties.Rule(25, 5),
                XpSource.SEGMENT_SHADOWING, new GamificationProperties.Rule(40, 5)
        ));

        listener = new LearningXpListener(
                checkInService,
                xpService,
                xpPolicy,
                props,
                gamificationEventPublisher
        );
    }

    @Nested
    @DisplayName("on")
    class OnTests {

        @Test
        @DisplayName("Process experience points event successfully with allowed policy")
        void on_Success_AllowedPolicy() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.SEGMENT_DICTATION, OCCURRED_AT);

            when(xpPolicy.allow(USER_ID, XpSource.SEGMENT_DICTATION, OCCURRED_AT.toLocalDate())).thenReturn(true);

            listener.on(event);

            verify(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);
            verify(xpPolicy).allow(USER_ID, XpSource.SEGMENT_DICTATION, OCCURRED_AT.toLocalDate());
            verify(xpService).addXp(USER_ID, 30, XpSource.SEGMENT_DICTATION, REF_ID);
            verify(gamificationEventPublisher).sendXpGained(USER_ID, 30);
        }

        @Test
        @DisplayName("Skip processing when rule is null")
        void on_Skip_RuleIsNull() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.BATTLE_WIN, OCCURRED_AT);

            listener.on(event);

            verify(checkInService, never()).ensureCheckedIn(anyLong(), any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Skip processing when source is daily check in")
        void on_Skip_DailyCheckIn() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.DAILY_CHECKIN, OCCURRED_AT);

            listener.on(event);

            verify(checkInService, never()).ensureCheckedIn(anyLong(), any());
            verify(xpPolicy, never()).allow(anyLong(), any(), any());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Skip adding experience when policy denies")
        void on_Skip_PolicyDenied() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT);

            when(xpPolicy.allow(USER_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT.toLocalDate())).thenReturn(false);

            listener.on(event);

            verify(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);
            verify(xpPolicy).allow(USER_ID, XpSource.FLASHCARD_REVIEW, OCCURRED_AT.toLocalDate());
            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }

        @Test
        @DisplayName("Process successfully when check in service throws exception then recovers")
        void on_Success_CheckInExceptionRecovered() {
            XpGainedEvent event = new XpGainedEvent(USER_ID, REF_ID, XpSource.SEGMENT_SHADOWING, OCCURRED_AT);

            doThrow(new RuntimeException("Redis down")).when(checkInService).ensureCheckedIn(USER_ID, OCCURRED_AT);

            listener.on(event);

            verify(xpService, never()).addXp(anyLong(), anyInt(), any(), anyLong());
            verify(gamificationEventPublisher, never()).sendXpGained(anyLong(), anyInt());
        }
    }
}
