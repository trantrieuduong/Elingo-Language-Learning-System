package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.request.RoundAnswerRecord;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.BattleAnswer;
import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleParticipant;
import com.elingo.battle.entity.BattleQuestion;
import com.elingo.battle.entity.BattleQuestionOption;
import com.elingo.battle.mapper.BattleMapper;
import com.elingo.battle.repository.BattleAnswerRepository;
import com.elingo.battle.repository.BattleMatchRepository;
import com.elingo.battle.repository.BattleParticipantRepository;
import com.elingo.battle.repository.BattleQuestionOptionRepository;
import com.elingo.battle.repository.BattleQuestionRepository;
import com.elingo.battle.service.impl.BattleMatchPersistenceServiceImpl;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleMatchPersistenceServiceTest {

    @Mock
    private BattleMatchRepository matchRepository;

    @Mock
    private BattleParticipantRepository participantRepository;

    @Mock
    private BattleAnswerRepository answerRepository;

    @Mock
    private BattleQuestionRepository questionRepository;

    @Mock
    private BattleQuestionOptionRepository optionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BattleMapper battleMapper;

    @Mock
    private BattleQuestionService questionService;

    @Captor
    private ArgumentCaptor<List<BattleParticipant>> participantsCaptor;

    @Captor
    private ArgumentCaptor<List<BattleAnswer>> answersCaptor;

    @InjectMocks
    private BattleMatchPersistenceServiceImpl persistenceService;

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Long MATCH_ID = 100L;
    private static final String ROOM_CODE = "ABC234";
    private static final BattleMode MODE = BattleMode.MULTIPLE_CHOICE;

    private static final BattleProperties PROPS = new BattleProperties(
            10, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(persistenceService, "battleProperties", PROPS);

        userA = User.builder().id(USER_A).username("user_a").fullName("User A").role(Role.USER).build();
        userB = User.builder().id(USER_B).username("user_b").fullName("User B").role(Role.USER).build();
    }

    private List<LiveQuestion> buildQuestions() {
        return List.of(new LiveQuestion(1001L, 1, "apple", "tao", 15000, List.of(
                new LiveQuestion.LiveOption(1L, "tao", true),
                new LiveQuestion.LiveOption(2L, "cam", false))));
    }

    /** Giả lập DB sinh id cho match và participants khi save */
    private void stubSaveMatchAndParticipants() {
        when(matchRepository.save(any(BattleMatch.class))).thenAnswer(inv -> {
            BattleMatch m = inv.getArgument(0);
            m.setId(MATCH_ID);
            return m;
        });
        when(participantRepository.saveAll(any())).thenAnswer(inv -> {
            List<BattleParticipant> list = inv.getArgument(0);
            list.get(0).setId(11L);
            list.get(1).setId(12L);
            return list;
        });
    }

    // ========================================================================
    // persistNewRandomMatch
    // ========================================================================
    @Nested
    @DisplayName("persistNewRandomMatch")
    class PersistNewRandomMatchTests {

        @Test
        @DisplayName("Persist new random match successfully")
        void persistNewRandomMatch_Success() {
            List<LiveQuestion> questions = buildQuestions();
            PlayerResponse playerA = new PlayerResponse(USER_A, "User A", null);
            PlayerResponse playerB = new PlayerResponse(USER_B, "User B", null);

            when(userRepository.findById(USER_A)).thenReturn(Optional.of(userA));
            when(userRepository.findById(USER_B)).thenReturn(Optional.of(userB));
            stubSaveMatchAndParticipants();
            when(questionService.createQuestions(any(BattleMatch.class))).thenReturn(questions);
            when(battleMapper.toPlayerResponse(userA)).thenReturn(playerA);
            when(battleMapper.toPlayerResponse(userB)).thenReturn(playerB);

            CreatedMatchResult result = persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE);

            assertThat(result.matchId()).isEqualTo(MATCH_ID);
            assertThat(result.mode()).isEqualTo(MODE);
            assertThat(result.userAId()).isEqualTo(USER_A);
            assertThat(result.userBId()).isEqualTo(USER_B);
            assertThat(result.players()).containsExactly(playerA, playerB);
            assertThat(result.participantIds()).isEqualTo(Map.of(USER_A, 11L, USER_B, 12L));
            assertThat(result.questions()).isEqualTo(questions);

            ArgumentCaptor<BattleMatch> matchCaptor = ArgumentCaptor.forClass(BattleMatch.class);
            verify(matchRepository).save(matchCaptor.capture());
            BattleMatch savedMatch = matchCaptor.getValue();
            assertThat(savedMatch.getMode()).isEqualTo(MODE);
            assertThat(savedMatch.getMatchType()).isEqualTo(BattleMatchType.RANDOM);
            assertThat(savedMatch.getRoomCode()).isNull();
            assertThat(savedMatch.getStatus()).isEqualTo(BattleMatchStatus.IN_PROGRESS);
            assertThat(savedMatch.getTotalRounds()).isEqualTo((short) PROPS.totalRounds());
            assertThat(savedMatch.getStartedAt()).isNotNull();

            verify(participantRepository).saveAll(participantsCaptor.capture());
            List<BattleParticipant> savedParticipants = participantsCaptor.getValue();
            assertThat(savedParticipants).hasSize(2);
            assertThat(savedParticipants.get(0).getUser()).isSameAs(userA);
            assertThat(savedParticipants.get(1).getUser()).isSameAs(userB);
            assertThat(savedParticipants).allSatisfy(p -> assertThat(p.getMatch()).isSameAs(savedMatch));

            verify(questionService).createQuestions(savedMatch);
        }

        @Test
        @DisplayName("Persist new random match failed: First user not found")
        void persistNewRandomMatch_Fail_UserANotFound() {
            when(userRepository.findById(USER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(matchRepository, never()).save(any(BattleMatch.class));
        }

        @Test
        @DisplayName("Persist new random match failed: Second user not found")
        void persistNewRandomMatch_Fail_UserBNotFound() {
            when(userRepository.findById(USER_A)).thenReturn(Optional.of(userA));
            when(userRepository.findById(USER_B)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(matchRepository, never()).save(any(BattleMatch.class));
        }

        @Test
        @DisplayName("Persist new random match failed: Question generation fails")
        void persistNewRandomMatch_Fail_QuestionGenerationFails() {
            when(userRepository.findById(USER_A)).thenReturn(Optional.of(userA));
            when(userRepository.findById(USER_B)).thenReturn(Optional.of(userB));
            when(matchRepository.save(any(BattleMatch.class))).thenAnswer(inv -> inv.getArgument(0));
            when(questionService.createQuestions(any(BattleMatch.class)))
                    .thenThrow(new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS));

            assertThatThrownBy(() -> persistenceService.persistNewRandomMatch(USER_A, USER_B, MODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_NOT_ENOUGH_CARDS));

            verify(battleMapper, never()).toPlayerResponse(any(User.class));
        }
    }

    // ========================================================================
    // persistNewRoomMatch
    // ========================================================================
    @Nested
    @DisplayName("persistNewRoomMatch")
    class PersistNewRoomMatchTests {

        @Test
        @DisplayName("Persist new room match successfully")
        void persistNewRoomMatch_Success() {
            when(userRepository.findById(USER_A)).thenReturn(Optional.of(userA));
            when(userRepository.findById(USER_B)).thenReturn(Optional.of(userB));
            stubSaveMatchAndParticipants();
            when(questionService.createQuestions(any(BattleMatch.class))).thenReturn(buildQuestions());
            when(battleMapper.toPlayerResponse(userA)).thenReturn(new PlayerResponse(USER_A, "User A", null));
            when(battleMapper.toPlayerResponse(userB)).thenReturn(new PlayerResponse(USER_B, "User B", null));

            CreatedMatchResult result = persistenceService.persistNewRoomMatch(USER_A, USER_B, MODE, ROOM_CODE);

            assertThat(result.matchId()).isEqualTo(MATCH_ID);
            assertThat(result.userAId()).isEqualTo(USER_A);
            assertThat(result.userBId()).isEqualTo(USER_B);

            ArgumentCaptor<BattleMatch> matchCaptor = ArgumentCaptor.forClass(BattleMatch.class);
            verify(matchRepository).save(matchCaptor.capture());
            assertThat(matchCaptor.getValue().getMatchType()).isEqualTo(BattleMatchType.ROOM);
            assertThat(matchCaptor.getValue().getRoomCode()).isEqualTo(ROOM_CODE);
            assertThat(matchCaptor.getValue().getStatus()).isEqualTo(BattleMatchStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Persist new room match failed: User not found")
        void persistNewRoomMatch_Fail_UserNotFound() {
            when(userRepository.findById(USER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> persistenceService.persistNewRoomMatch(USER_A, USER_B, MODE, ROOM_CODE))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(matchRepository, never()).save(any(BattleMatch.class));
        }
    }

    // ========================================================================
    // roomCodeExists
    // ========================================================================
    @Nested
    @DisplayName("roomCodeExists")
    class RoomCodeExistsTests {

        @Test
        @DisplayName("Room code exists")
        void roomCodeExists_True() {
            when(matchRepository.existsByRoomCode(ROOM_CODE)).thenReturn(true);

            assertThat(persistenceService.roomCodeExists(ROOM_CODE)).isTrue();
        }

        @Test
        @DisplayName("Room code does not exist")
        void roomCodeExists_False() {
            when(matchRepository.existsByRoomCode(ROOM_CODE)).thenReturn(false);

            assertThat(persistenceService.roomCodeExists(ROOM_CODE)).isFalse();
        }
    }

    // ========================================================================
    // updateMatchStatus
    // ========================================================================
    @Nested
    @DisplayName("updateMatchStatus")
    class UpdateMatchStatusTests {

        private BattleMatch inProgressMatch() {
            return BattleMatch.builder().id(MATCH_ID).mode(MODE).matchType(BattleMatchType.RANDOM)
                    .status(BattleMatchStatus.IN_PROGRESS).build();
        }

        @Test
        @DisplayName("Update match status to CANCELLED sets finished time")
        void updateMatchStatus_Success_Cancelled() {
            BattleMatch match = inProgressMatch();
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

            persistenceService.updateMatchStatus(MATCH_ID, BattleMatchStatus.CANCELLED);

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.CANCELLED);
            assertThat(match.getFinishedAt()).isNotNull();
            verify(matchRepository).save(match);
        }

        @Test
        @DisplayName("Update match status to FINISHED sets finished time")
        void updateMatchStatus_Success_Finished() {
            BattleMatch match = inProgressMatch();
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

            persistenceService.updateMatchStatus(MATCH_ID, BattleMatchStatus.FINISHED);

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);
            assertThat(match.getFinishedAt()).isNotNull();
            verify(matchRepository).save(match);
        }

        @Test
        @DisplayName("Update match status to IN_PROGRESS does not set finished time")
        void updateMatchStatus_Success_InProgress() {
            BattleMatch match = BattleMatch.builder().id(MATCH_ID).mode(MODE).matchType(BattleMatchType.RANDOM)
                    .status(BattleMatchStatus.WAITING).build();
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

            persistenceService.updateMatchStatus(MATCH_ID, BattleMatchStatus.IN_PROGRESS);

            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.IN_PROGRESS);
            assertThat(match.getFinishedAt()).isNull();
            verify(matchRepository).save(match);
        }

        @Test
        @DisplayName("Update match status does nothing when match not found")
        void updateMatchStatus_Success_MatchNotFound() {
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

            persistenceService.updateMatchStatus(MATCH_ID, BattleMatchStatus.CANCELLED);

            verify(matchRepository, never()).save(any(BattleMatch.class));
        }
    }

    // ========================================================================
    // saveRoundAnswers
    // ========================================================================
    @Nested
    @DisplayName("saveRoundAnswers")
    class SaveRoundAnswersTests {

        @Test
        @DisplayName("Save round answers successfully with selected option, typed answer and timeout")
        void saveRoundAnswers_Success() {
            BattleQuestion question = BattleQuestion.builder().id(1001L).build();
            BattleParticipant participantA = BattleParticipant.builder().id(11L).build();
            BattleParticipant participantB = BattleParticipant.builder().id(12L).build();
            BattleQuestionOption option = BattleQuestionOption.builder().id(5001L).build();

            when(questionRepository.getReferenceById(1001L)).thenReturn(question);
            when(participantRepository.getReferenceById(11L)).thenReturn(participantA);
            when(participantRepository.getReferenceById(12L)).thenReturn(participantB);
            when(optionRepository.getReferenceById(5001L)).thenReturn(option);

            List<RoundAnswerRecord> records = List.of(
                    new RoundAnswerRecord(1001L, 11L, 5001L, null, true, 2000),
                    new RoundAnswerRecord(1001L, 12L, null, null, false, 15000)
            );

            persistenceService.saveRoundAnswers(records);

            verify(answerRepository).saveAll(answersCaptor.capture());
            List<BattleAnswer> saved = answersCaptor.getValue();
            assertThat(saved).hasSize(2);

            BattleAnswer first = saved.get(0);
            assertThat(first.getQuestion()).isSameAs(question);
            assertThat(first.getParticipant()).isSameAs(participantA);
            assertThat(first.getSelectedOption()).isSameAs(option);
            assertThat(first.getTypedAnswer()).isNull();
            assertThat(first.getIsCorrect()).isTrue();
            assertThat(first.getResponseTimeMs()).isEqualTo(2000);

            BattleAnswer second = saved.get(1);
            assertThat(second.getParticipant()).isSameAs(participantB);
            assertThat(second.getSelectedOption()).isNull();
            assertThat(second.getTypedAnswer()).isNull();
            assertThat(second.getIsCorrect()).isFalse();
            assertThat(second.getResponseTimeMs()).isEqualTo(15000);

            // Chỉ record có selectedOptionId mới cần tham chiếu option
            verify(optionRepository, times(1)).getReferenceById(any());
        }

        @Test
        @DisplayName("Save round answers successfully with typed answers")
        void saveRoundAnswers_Success_TypedAnswer() {
            BattleQuestion question = BattleQuestion.builder().id(1001L).build();
            BattleParticipant participantA = BattleParticipant.builder().id(11L).build();

            when(questionRepository.getReferenceById(1001L)).thenReturn(question);
            when(participantRepository.getReferenceById(11L)).thenReturn(participantA);

            persistenceService.saveRoundAnswers(List.of(
                    new RoundAnswerRecord(1001L, 11L, null, "ice cream", true, 4000)));

            verify(answerRepository).saveAll(answersCaptor.capture());
            BattleAnswer saved = answersCaptor.getValue().get(0);
            assertThat(saved.getTypedAnswer()).isEqualTo("ice cream");
            assertThat(saved.getSelectedOption()).isNull();
            assertThat(saved.getIsCorrect()).isTrue();
            assertThat(saved.getResponseTimeMs()).isEqualTo(4000);
        }

        @Test
        @DisplayName("Save round answers with empty records saves an empty list")
        void saveRoundAnswers_Success_Empty() {
            persistenceService.saveRoundAnswers(List.of());

            verify(answerRepository).saveAll(answersCaptor.capture());
            assertThat(answersCaptor.getValue()).isEmpty();
        }
    }

    // ========================================================================
    // finishMatch
    // ========================================================================
    @Nested
    @DisplayName("finishMatch")
    class FinishMatchTests {

        private BattleMatch match;
        private BattleParticipant participantA;
        private BattleParticipant participantB;

        @BeforeEach
        void initMatch() {
            match = BattleMatch.builder().id(MATCH_ID).mode(MODE).matchType(BattleMatchType.RANDOM)
                    .status(BattleMatchStatus.IN_PROGRESS).build();
            participantA = BattleParticipant.builder().id(11L).match(match).user(userA).build();
            participantB = BattleParticipant.builder().id(12L).match(match).user(userB).build();
        }

        @Test
        @DisplayName("Finish match successfully with a winner")
        void finishMatch_Success_WithWinner() {
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID)).thenReturn(List.of(participantA, participantB));
            when(userRepository.getReferenceById(USER_A)).thenReturn(userA);

            persistenceService.finishMatch(MATCH_ID, USER_A,
                    Map.of(USER_A, 450, USER_B, 300), Map.of(USER_A, 4, USER_B, 3));

            assertThat(participantA.getScore()).isEqualTo(450);
            assertThat(participantA.getCorrectCount()).isEqualTo(4);
            assertThat(participantA.getIsWinner()).isTrue();
            assertThat(participantB.getScore()).isEqualTo(300);
            assertThat(participantB.getCorrectCount()).isEqualTo(3);
            assertThat(participantB.getIsWinner()).isFalse();
            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);
            assertThat(match.getFinishedAt()).isNotNull();
            assertThat(match.getWinner()).isSameAs(userA);
        }

        @Test
        @DisplayName("Finish match successfully as a draw without winner")
        void finishMatch_Success_Draw() {
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID)).thenReturn(List.of(participantA, participantB));

            persistenceService.finishMatch(MATCH_ID, null,
                    Map.of(USER_A, 200, USER_B, 200), Map.of(USER_A, 2, USER_B, 2));

            assertThat(participantA.getIsWinner()).isFalse();
            assertThat(participantB.getIsWinner()).isFalse();
            assertThat(match.getStatus()).isEqualTo(BattleMatchStatus.FINISHED);
            assertThat(match.getWinner()).isNull();
            verify(userRepository, never()).getReferenceById(any());
        }

        @Test
        @DisplayName("Finish match defaults missing scores to zero")
        void finishMatch_Success_MissingScoresDefaultToZero() {
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID)).thenReturn(List.of(participantA, participantB));
            when(userRepository.getReferenceById(USER_A)).thenReturn(userA);

            persistenceService.finishMatch(MATCH_ID, USER_A, Map.of(USER_A, 100), Map.of(USER_A, 1));

            assertThat(participantB.getScore()).isZero();
            assertThat(participantB.getCorrectCount()).isZero();
        }

        @Test
        @DisplayName("Finish match failed: Match not found")
        void finishMatch_Fail_MatchNotFound() {
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> persistenceService.finishMatch(MATCH_ID, USER_A, Map.of(), Map.of()))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));

            verify(participantRepository, never()).findByMatchIdWithUser(any());
        }
    }

    // ========================================================================
    // cancelOrphanMatches
    // ========================================================================
    @Nested
    @DisplayName("cancelOrphanMatches")
    class CancelOrphanMatchesTests {

        @Test
        @DisplayName("Cancel orphan matches returns the number of cancelled matches")
        void cancelOrphanMatches_Success() {
            when(matchRepository.cancelAllUnfinished(any(LocalDateTime.class))).thenReturn(3);

            int cancelled = persistenceService.cancelOrphanMatches();

            assertThat(cancelled).isEqualTo(3);
        }

        @Test
        @DisplayName("Cancel orphan matches returns zero when nothing to cancel")
        void cancelOrphanMatches_Success_None() {
            when(matchRepository.cancelAllUnfinished(any(LocalDateTime.class))).thenReturn(0);

            assertThat(persistenceService.cancelOrphanMatches()).isZero();
        }
    }
}
