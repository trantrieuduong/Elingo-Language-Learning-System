package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.dto.response.MatchFoundResponse;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.service.impl.BattleMatchServiceImpl;
import com.elingo.battle.state.BattleLiveRegistry;
import com.elingo.battle.state.LiveMatch;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.battle.websocket.BattleEvents;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleMatchServiceTest {

    @Mock
    private BattleMatchPersistenceService persistenceService;

    @Mock
    private BattleEventPublisher eventPublisher;

    @Mock
    private BattleLiveRegistry registry;

    @Mock
    private BattleEngineService engineService;

    @InjectMocks
    private BattleMatchServiceImpl matchService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long MATCH_ID = 100L;
    private static final String ROOM_CODE = "ABC234";
    private static final BattleMode MODE = BattleMode.MULTIPLE_CHOICE;

    private static final BattleProperties PROPS = new BattleProperties(
            10, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    private List<PlayerResponse> players;
    private Map<Long, Long> participantIds;
    private List<LiveQuestion> questions;
    private CreatedMatchResult createdMatchResult;

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(matchService, "battleProperties", PROPS);

        players = List.of(
                new PlayerResponse(USER_A, "User A", null),
                new PlayerResponse(USER_B, "User B", null)
        );
        participantIds = Map.of(USER_A, 11L, USER_B, 12L);
        questions = List.of(
                new LiveQuestion(1001L, 1, "apple", "tao", 15000, List.of(
                        new LiveQuestion.LiveOption(1L, "tao", true),
                        new LiveQuestion.LiveOption(2L, "cam", false),
                        new LiveQuestion.LiveOption(3L, "chuoi", false),
                        new LiveQuestion.LiveOption(4L, "nho", false)))
        );
        createdMatchResult = new CreatedMatchResult(MATCH_ID, MODE, USER_A, USER_B, players, participantIds, questions);
    }

    // ========================================================================
    // createRandomMatch
    // ========================================================================
    @Nested
    @DisplayName("createRandomMatch")
    class CreateRandomMatchTests {

        @Test
        @DisplayName("Create random match successfully")
        void createRandomMatch_Success() {
            when(persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE)).thenReturn(createdMatchResult);

            matchService.createRandomMatch(USER_A, USER_B, MODE);

            ArgumentCaptor<LiveMatch> matchCaptor = ArgumentCaptor.forClass(LiveMatch.class);
            verify(registry).register(matchCaptor.capture());
            LiveMatch live = matchCaptor.getValue();
            assertThat(live.getMatchId()).isEqualTo(MATCH_ID);
            assertThat(live.getMode()).isEqualTo(MODE);
            assertThat(live.getMatchType()).isEqualTo(BattleMatchType.RANDOM);
            assertThat(live.getTotalRounds()).isEqualTo(PROPS.totalRounds());
            assertThat(live.getUserAId()).isEqualTo(USER_A);
            assertThat(live.getUserBId()).isEqualTo(USER_B);
            assertThat(live.getParticipantIds()).isEqualTo(participantIds);
            assertThat(live.getQuestions()).isEqualTo(questions);
            assertThat(live.getPlayers()).isEqualTo(players);
            assertThat(live.getStatus()).isEqualTo(BattleMatchStatus.IN_PROGRESS);

            MatchFoundResponse expectedFound = new MatchFoundResponse(
                    MATCH_ID, MODE, BattleMatchType.RANDOM, PROPS.totalRounds(), PROPS.countdownMs(), players);
            verify(eventPublisher).sendToUser(USER_A, BattleEvents.MATCH_FOUND, expectedFound);
            verify(eventPublisher).sendToUser(USER_B, BattleEvents.MATCH_FOUND, expectedFound);
            verify(engineService).start(live);
        }

        @Test
        @DisplayName("Create random match persists first, then registers, notifies and starts the engine")
        void createRandomMatch_Success_CallOrder() {
            when(persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE)).thenReturn(createdMatchResult);

            matchService.createRandomMatch(USER_A, USER_B, MODE);

            InOrder inOrder = inOrder(persistenceService, registry, eventPublisher, engineService);
            inOrder.verify(persistenceService).persistNewRandomMatch(USER_A, USER_B, MODE);
            inOrder.verify(registry).register(any(LiveMatch.class));
            inOrder.verify(eventPublisher, times(2))
                    .sendToUser(anyLong(), eq(BattleEvents.MATCH_FOUND), any());
            inOrder.verify(engineService).start(any(LiveMatch.class));
        }

        @Test
        @DisplayName("Create random match failed: Persisting the match fails")
        void createRandomMatch_Fail_PersistError() {
            when(persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE))
                    .thenThrow(new AppException(AppError.USER_NOT_FOUND));

            assertThatThrownBy(() -> matchService.createRandomMatch(USER_A, USER_B, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verifyNoInteractions(registry);
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(engineService);
        }
    }

    // ========================================================================
    // createRoomMatch
    // ========================================================================
    @Nested
    @DisplayName("createRoomMatch")
    class CreateRoomMatchTests {

        @Test
        @DisplayName("Create room match successfully")
        void createRoomMatch_Success() {
            when(persistenceService.persistNewRoomMatch(USER_A, USER_B, MODE, ROOM_CODE)).thenReturn(createdMatchResult);

            matchService.createRoomMatch(USER_A, USER_B, MODE, ROOM_CODE);

            ArgumentCaptor<LiveMatch> matchCaptor = ArgumentCaptor.forClass(LiveMatch.class);
            verify(registry).register(matchCaptor.capture());
            LiveMatch live = matchCaptor.getValue();
            assertThat(live.getMatchId()).isEqualTo(MATCH_ID);
            assertThat(live.getMatchType()).isEqualTo(BattleMatchType.ROOM);
            assertThat(live.getUserAId()).isEqualTo(USER_A);
            assertThat(live.getUserBId()).isEqualTo(USER_B);

            MatchFoundResponse expectedFound = new MatchFoundResponse(
                    MATCH_ID, MODE, BattleMatchType.ROOM, PROPS.totalRounds(), PROPS.countdownMs(), players);
            verify(eventPublisher).sendToUser(USER_A, BattleEvents.MATCH_FOUND, expectedFound);
            verify(eventPublisher).sendToUser(USER_B, BattleEvents.MATCH_FOUND, expectedFound);
            verify(engineService).start(live);
        }

        @Test
        @DisplayName("Create room match failed: Persisting the match fails")
        void createRoomMatch_Fail_PersistError() {
            when(persistenceService.persistNewRoomMatch(USER_A, USER_B, MODE, ROOM_CODE))
                    .thenThrow(new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS));

            assertThatThrownBy(() -> matchService.createRoomMatch(USER_A, USER_B, MODE, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_NOT_ENOUGH_CARDS));

            verifyNoInteractions(registry);
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(engineService);
        }
    }

    // ========================================================================
    // isUserInActiveMatch
    // ========================================================================
    @Nested
    @DisplayName("isUserInActiveMatch")
    class IsUserInActiveMatchTests {

        @Test
        @DisplayName("User is in active match")
        void isUserInActiveMatch_True() {
            when(registry.isUserInActiveMatch(USER_A)).thenReturn(true);

            assertThat(matchService.isUserInActiveMatch(USER_A)).isTrue();
        }

        @Test
        @DisplayName("User is not in active match")
        void isUserInActiveMatch_False() {
            when(registry.isUserInActiveMatch(USER_A)).thenReturn(false);

            assertThat(matchService.isUserInActiveMatch(USER_A)).isFalse();
        }
    }
}
