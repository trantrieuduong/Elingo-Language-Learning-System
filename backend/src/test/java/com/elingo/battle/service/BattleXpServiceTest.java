package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.impl.BattleXpServiceImpl;
import com.elingo.battle.state.LiveMatch;
import com.elingo.common.event.XpGainedEvent;
import com.elingo.gamification.entity.XpSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleXpServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BattleXpServiceImpl xpService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long MATCH_ID = 100L;

    private static final BattleProperties PROPS = new BattleProperties(
            10, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(xpService, "props", PROPS);
    }

    private LiveMatch buildMatch(BattleMatchType type, int correctA, int correctB) {
        LiveMatch match = new LiveMatch(MATCH_ID, BattleMode.MULTIPLE_CHOICE, type, 10,
                USER_A, USER_B, Map.of(USER_A, 11L, USER_B, 12L), List.of());
        for (int i = 0; i < correctA; i++) {
            match.addScore(USER_A, 100, true);
        }
        for (int i = 0; i < correctB; i++) {
            match.addScore(USER_B, 100, true);
        }
        return match;
    }

    private List<XpGainedEvent> captureEvents(int expectedCount) {
        ArgumentCaptor<XpGainedEvent> captor = ArgumentCaptor.forClass(XpGainedEvent.class);
        verify(eventPublisher, times(expectedCount)).publishEvent(captor.capture());
        return captor.getAllValues();
    }

    // ========================================================================
    // awardMatchXp
    // ========================================================================
    @Nested
    @DisplayName("awardMatchXp")
    class AwardMatchXpTests {

        @Test
        @DisplayName("Award BATTLE_WIN to winner and BATTLE_LOSE to loser in random match")
        void awardMatchXp_Success_WinnerAndLoser() {
            LiveMatch match = buildMatch(BattleMatchType.RANDOM, 5, 3);

            xpService.awardMatchXp(match, USER_A, "COMPLETED");

            List<XpGainedEvent> events = captureEvents(2);
            assertThat(events.get(0).userId()).isEqualTo(USER_A);
            assertThat(events.get(0).refId()).isEqualTo(MATCH_ID);
            assertThat(events.get(0).xpSource()).isEqualTo(XpSource.BATTLE_WIN);
            assertThat(events.get(1).userId()).isEqualTo(USER_B);
            assertThat(events.get(1).refId()).isEqualTo(MATCH_ID);
            assertThat(events.get(1).xpSource()).isEqualTo(XpSource.BATTLE_LOSE);
        }

        @Test
        @DisplayName("Award BATTLE_LOSE to both users when match is a draw")
        void awardMatchXp_Success_Draw() {
            LiveMatch match = buildMatch(BattleMatchType.RANDOM, 4, 4);

            xpService.awardMatchXp(match, null, "COMPLETED");

            List<XpGainedEvent> events = captureEvents(2);
            assertThat(events).extracting(XpGainedEvent::xpSource)
                    .containsExactly(XpSource.BATTLE_LOSE, XpSource.BATTLE_LOSE);
        }

        @Test
        @DisplayName("Award XP when correct count equals the minimum threshold")
        void awardMatchXp_Success_ExactlyMinCorrect() {
            LiveMatch match = buildMatch(BattleMatchType.RANDOM, PROPS.minCorrectForXp(), PROPS.minCorrectForXp());

            xpService.awardMatchXp(match, USER_B, "COMPLETED");

            List<XpGainedEvent> events = captureEvents(2);
            assertThat(events.get(0).xpSource()).isEqualTo(XpSource.BATTLE_LOSE);
            assertThat(events.get(1).xpSource()).isEqualTo(XpSource.BATTLE_WIN);
        }

        @Test
        @DisplayName("Skip user whose correct count is below the minimum threshold")
        void awardMatchXp_Success_SkipBelowMinCorrect() {
            LiveMatch match = buildMatch(BattleMatchType.RANDOM, PROPS.minCorrectForXp() - 1, 5);

            xpService.awardMatchXp(match, USER_B, "COMPLETED");

            List<XpGainedEvent> events = captureEvents(1);
            assertThat(events.get(0).userId()).isEqualTo(USER_B);
            assertThat(events.get(0).xpSource()).isEqualTo(XpSource.BATTLE_WIN);
        }

        @Test
        @DisplayName("Award no XP when nobody reaches the minimum correct count")
        void awardMatchXp_Success_NobodyQualifies() {
            LiveMatch match = buildMatch(BattleMatchType.RANDOM, 1, 2);

            xpService.awardMatchXp(match, USER_B, "COMPLETED");

            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Award XP only to winner when the other user forfeits")
        void awardMatchXp_Success_ForfeitSkipsLoser() {
            LiveMatch match = buildMatch(BattleMatchType.RANDOM, 5, 5);

            xpService.awardMatchXp(match, USER_A, "FORFEIT");

            List<XpGainedEvent> events = captureEvents(1);
            assertThat(events.get(0).userId()).isEqualTo(USER_A);
            assertThat(events.get(0).xpSource()).isEqualTo(XpSource.BATTLE_WIN);
        }

        @Test
        @DisplayName("Award no XP for room match")
        void awardMatchXp_Success_RoomMatchIgnored() {
            LiveMatch match = buildMatch(BattleMatchType.ROOM, 5, 5);

            xpService.awardMatchXp(match, USER_A, "COMPLETED");

            verifyNoInteractions(eventPublisher);
        }
    }
}
