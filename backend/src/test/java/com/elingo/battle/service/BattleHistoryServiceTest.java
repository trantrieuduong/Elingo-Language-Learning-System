package com.elingo.battle.service;

import com.elingo.battle.dto.response.BattleHistoryItemResponse;
import com.elingo.battle.dto.response.BattleMatchDetailResponse;
import com.elingo.battle.dto.response.BattleRoundAnswerResponse;
import com.elingo.battle.dto.response.BattleStatsResponse;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.BattleAnswer;
import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleParticipant;
import com.elingo.battle.entity.BattleQuestion;
import com.elingo.battle.entity.BattleQuestionOption;
import com.elingo.battle.entity.BattleResult;
import com.elingo.battle.mapper.BattleMapper;
import com.elingo.battle.repository.BattleAnswerRepository;
import com.elingo.battle.repository.BattleMatchRepository;
import com.elingo.battle.repository.BattleParticipantRepository;
import com.elingo.battle.repository.BattleQuestionRepository;
import com.elingo.battle.service.impl.BattleHistoryServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleHistoryServiceTest {

    @Mock
    private BattleParticipantRepository participantRepository;

    @Mock
    private BattleMatchRepository matchRepository;

    @Mock
    private BattleQuestionRepository questionRepository;

    @Mock
    private BattleAnswerRepository answerRepository;

    @Mock
    private BattleMapper battleMapper;

    @InjectMocks
    private BattleHistoryServiceImpl battleHistoryService;

    private static final Long USER_ID = 1L;
    private static final Long OPPONENT_ID = 2L;
    private static final Long OTHER_USER_ID = 3L;
    private static final Long MATCH_ID = 100L;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private User testUser;
    private User opponentUser;
    private BattleMatch testMatch;
    private BattleParticipant myParticipant;
    private BattleParticipant opponentParticipant;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username("user1")
                .fullName("User One")
                .role(Role.USER)
                .build();

        opponentUser = User.builder()
                .id(OPPONENT_ID)
                .username("opponent")
                .fullName("Opponent")
                .role(Role.USER)
                .build();

        testMatch = BattleMatch.builder()
                .id(MATCH_ID)
                .mode(BattleMode.MULTIPLE_CHOICE)
                .matchType(BattleMatchType.RANDOM)
                .status(BattleMatchStatus.FINISHED)
                .winner(testUser)
                .totalRounds((short) 2)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .finishedAt(LocalDateTime.now())
                .build();

        myParticipant = BattleParticipant.builder()
                .id(11L)
                .match(testMatch)
                .user(testUser)
                .score(300)
                .correctCount(2)
                .isWinner(true)
                .build();

        opponentParticipant = BattleParticipant.builder()
                .id(12L)
                .match(testMatch)
                .user(opponentUser)
                .score(150)
                .correctCount(1)
                .isWinner(false)
                .build();
    }

    private PlayerResponse toPlayerResponse(User user) {
        return new PlayerResponse(user.getId(), user.getFullName(), null);
    }

    private Page<BattleParticipant> pageOf(BattleParticipant... participants) {
        return new PageImpl<>(List.of(participants), PAGEABLE, participants.length);
    }

    private BattleParticipantRepository.BattleStatsProjection projection(long total, long wins, long draws) {
        return new BattleParticipantRepository.BattleStatsProjection() {
            @Override
            public long getTotal() {
                return total;
            }

            @Override
            public long getWins() {
                return wins;
            }

            @Override
            public long getDraws() {
                return draws;
            }
        };
    }

    // ========================================================================
    // getHistory
    // ========================================================================
    @Nested
    @DisplayName("getHistory")
    class GetHistoryTests {

        @Test
        @DisplayName("Get history successfully with a won match")
        void getHistory_Success_Win() {
            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(pageOf(myParticipant));
            when(participantRepository.findByMatchIds(eq(List.of(MATCH_ID))))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            when(battleMapper.toPlayerResponse(opponentUser)).thenReturn(toPlayerResponse(opponentUser));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            assertThat(response.content()).hasSize(1);
            assertThat(response.page()).isEqualTo(1);
            assertThat(response.totalElements()).isEqualTo(1);

            BattleHistoryItemResponse item = response.content().get(0);
            assertThat(item.matchId()).isEqualTo(MATCH_ID);
            assertThat(item.mode()).isEqualTo(BattleMode.MULTIPLE_CHOICE);
            assertThat(item.matchType()).isEqualTo(BattleMatchType.RANDOM);
            assertThat(item.result()).isEqualTo(BattleResult.WIN);
            assertThat(item.myScore()).isEqualTo(300);
            assertThat(item.opponentScore()).isEqualTo(150);
            assertThat(item.opponent().userId()).isEqualTo(OPPONENT_ID);
            assertThat(item.finishedAt()).isEqualTo(testMatch.getFinishedAt());
        }

        @Test
        @DisplayName("Get history successfully with a lost match")
        void getHistory_Success_Lose() {
            testMatch.setWinner(opponentUser);
            myParticipant.setScore(100);
            myParticipant.setIsWinner(false);
            opponentParticipant.setScore(250);
            opponentParticipant.setIsWinner(true);

            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(pageOf(myParticipant));
            when(participantRepository.findByMatchIds(eq(List.of(MATCH_ID))))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            when(battleMapper.toPlayerResponse(opponentUser)).thenReturn(toPlayerResponse(opponentUser));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            BattleHistoryItemResponse item = response.content().get(0);
            assertThat(item.result()).isEqualTo(BattleResult.LOSE);
            assertThat(item.myScore()).isEqualTo(100);
            assertThat(item.opponentScore()).isEqualTo(250);
        }

        @Test
        @DisplayName("Get history successfully with a drawn match")
        void getHistory_Success_Draw() {
            testMatch.setWinner(null);
            myParticipant.setScore(200);
            myParticipant.setIsWinner(false);
            opponentParticipant.setScore(200);
            opponentParticipant.setIsWinner(false);

            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(pageOf(myParticipant));
            when(participantRepository.findByMatchIds(eq(List.of(MATCH_ID))))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            when(battleMapper.toPlayerResponse(opponentUser)).thenReturn(toPlayerResponse(opponentUser));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            BattleHistoryItemResponse item = response.content().get(0);
            assertThat(item.result()).isEqualTo(BattleResult.DRAW);
            assertThat(item.myScore()).isEqualTo(200);
            assertThat(item.opponentScore()).isEqualTo(200);
        }

        @Test
        @DisplayName("Get history successfully with multiple matches and different opponents")
        void getHistory_Success_MultipleMatches() {
            BattleMatch secondMatch = BattleMatch.builder()
                    .id(MATCH_ID + 1)
                    .mode(BattleMode.TYPING)
                    .matchType(BattleMatchType.ROOM)
                    .status(BattleMatchStatus.FINISHED)
                    .winner(null)
                    .totalRounds((short) 2)
                    .finishedAt(LocalDateTime.now().minusDays(1))
                    .build();
            User otherUser = User.builder().id(OTHER_USER_ID).fullName("Other").role(Role.USER).build();
            BattleParticipant mySecond = BattleParticipant.builder()
                    .id(21L).match(secondMatch).user(testUser).score(50).isWinner(false).build();
            BattleParticipant otherSecond = BattleParticipant.builder()
                    .id(22L).match(secondMatch).user(otherUser).score(50).isWinner(false).build();

            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(new PageImpl<>(List.of(myParticipant, mySecond), PAGEABLE, 2));
            when(participantRepository.findByMatchIds(eq(List.of(MATCH_ID, MATCH_ID + 1))))
                    .thenReturn(List.of(myParticipant, opponentParticipant, mySecond, otherSecond));
            when(battleMapper.toPlayerResponse(opponentUser)).thenReturn(toPlayerResponse(opponentUser));
            when(battleMapper.toPlayerResponse(otherUser)).thenReturn(toPlayerResponse(otherUser));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            assertThat(response.content()).hasSize(2);
            assertThat(response.content().get(0).matchId()).isEqualTo(MATCH_ID);
            assertThat(response.content().get(0).opponent().userId()).isEqualTo(OPPONENT_ID);
            assertThat(response.content().get(0).result()).isEqualTo(BattleResult.WIN);
            assertThat(response.content().get(1).matchId()).isEqualTo(MATCH_ID + 1);
            assertThat(response.content().get(1).opponent().userId()).isEqualTo(OTHER_USER_ID);
            assertThat(response.content().get(1).result()).isEqualTo(BattleResult.DRAW);
            assertThat(response.content().get(1).mode()).isEqualTo(BattleMode.TYPING);
            assertThat(response.content().get(1).matchType()).isEqualTo(BattleMatchType.ROOM);
        }

        @Test
        @DisplayName("Get history successfully with null scores defaulting to zero")
        void getHistory_Success_NullScores() {
            myParticipant.setScore(null);
            opponentParticipant.setScore(null);

            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(pageOf(myParticipant));
            when(participantRepository.findByMatchIds(eq(List.of(MATCH_ID))))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            when(battleMapper.toPlayerResponse(opponentUser)).thenReturn(toPlayerResponse(opponentUser));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            assertThat(response.content().get(0).myScore()).isZero();
            assertThat(response.content().get(0).opponentScore()).isZero();
        }

        @Test
        @DisplayName("Get history successfully when opponent participant is missing")
        void getHistory_Success_OpponentMissing() {
            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(pageOf(myParticipant));
            when(participantRepository.findByMatchIds(eq(List.of(MATCH_ID))))
                    .thenReturn(List.of(myParticipant));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            BattleHistoryItemResponse item = response.content().get(0);
            assertThat(item.opponent()).isNull();
            assertThat(item.opponentScore()).isZero();
            verify(battleMapper, never()).toPlayerResponse(any(User.class));
        }

        @Test
        @DisplayName("Get history successfully with empty result")
        void getHistory_Success_Empty() {
            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            PageResponse<BattleHistoryItemResponse> response =
                    battleHistoryService.getHistory(USER_ID, null, null, null, PAGEABLE);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            verify(participantRepository, never()).findByMatchIds(any());
            verify(battleMapper, never()).toPlayerResponse(any(User.class));
        }

        @Test
        @DisplayName("Get history maps WIN filter to 1")
        void getHistory_FilterWin() {
            when(participantRepository.findHistory(eq(USER_ID), eq(1), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            battleHistoryService.getHistory(USER_ID, BattleResult.WIN, null, null, PAGEABLE);

            verify(participantRepository).findHistory(USER_ID, 1, null, null, PAGEABLE);
        }

        @Test
        @DisplayName("Get history maps LOSE filter to 2")
        void getHistory_FilterLose() {
            when(participantRepository.findHistory(eq(USER_ID), eq(2), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            battleHistoryService.getHistory(USER_ID, BattleResult.LOSE, null, null, PAGEABLE);

            verify(participantRepository).findHistory(USER_ID, 2, null, null, PAGEABLE);
        }

        @Test
        @DisplayName("Get history maps DRAW filter to 3")
        void getHistory_FilterDraw() {
            when(participantRepository.findHistory(eq(USER_ID), eq(3), isNull(), isNull(), eq(PAGEABLE)))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            battleHistoryService.getHistory(USER_ID, BattleResult.DRAW, null, null, PAGEABLE);

            verify(participantRepository).findHistory(USER_ID, 3, null, null, PAGEABLE);
        }

        @Test
        @DisplayName("Get history passes mode and match type filters to repository")
        void getHistory_PassesModeAndMatchType() {
            when(participantRepository.findHistory(eq(USER_ID), eq(0), eq(BattleMode.TYPING),
                    eq(BattleMatchType.ROOM), eq(PAGEABLE)))
                    .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

            battleHistoryService.getHistory(USER_ID, null, BattleMode.TYPING, BattleMatchType.ROOM, PAGEABLE);

            verify(participantRepository).findHistory(USER_ID, 0, BattleMode.TYPING, BattleMatchType.ROOM, PAGEABLE);
        }

        @Test
        @DisplayName("Get history ignores sort from client and only keeps page number and size")
        void getHistory_IgnoresClientSort() {
            Pageable sortedPageable = PageRequest.of(2, 5, Sort.by("id").descending());
            Pageable expectedPageable = PageRequest.of(2, 5);

            when(participantRepository.findHistory(eq(USER_ID), eq(0), isNull(), isNull(), eq(expectedPageable)))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            battleHistoryService.getHistory(USER_ID, null, null, null, sortedPageable);

            verify(participantRepository).findHistory(USER_ID, 0, null, null, expectedPageable);
        }
    }

    // ========================================================================
    // getMatchDetail
    // ========================================================================
    @Nested
    @DisplayName("getMatchDetail")
    class GetMatchDetailTests {

        private BattleQuestion question1;
        private BattleQuestion question2;

        @BeforeEach
        void initQuestions() {
            question1 = BattleQuestion.builder()
                    .id(1001L).match(testMatch).roundNo((short) 1).term("apple").correctAnswer("tao").build();
            question2 = BattleQuestion.builder()
                    .id(1002L).match(testMatch).roundNo((short) 2).term("banana").correctAnswer("chuoi").build();
        }

        private BattleAnswer buildAnswer(BattleQuestion question, BattleParticipant participant,
                                         BattleQuestionOption option, String typed, boolean correct, int responseTimeMs) {
            return BattleAnswer.builder()
                    .question(question)
                    .participant(participant)
                    .selectedOption(option)
                    .typedAnswer(typed)
                    .isCorrect(correct)
                    .responseTimeMs(responseTimeMs)
                    .build();
        }

        private void stubMappers() {
            when(battleMapper.toPlayerResponse(any(User.class))).thenAnswer(inv -> toPlayerResponse(inv.getArgument(0)));
        }

        @Test
        @DisplayName("Get match detail successfully with multiple choice answers and unanswered round")
        void getMatchDetail_Success_MultipleChoice() {
            BattleQuestionOption correctOption = BattleQuestionOption.builder()
                    .id(5001L).question(question1).optionText("tao").isCorrect(true).build();
            BattleQuestionOption wrongOption = BattleQuestionOption.builder()
                    .id(5002L).question(question1).optionText("cam").isCorrect(false).build();

            BattleAnswer myAnswer = buildAnswer(question1, myParticipant, correctOption, null, true, 2000);
            BattleAnswer opponentAnswer = buildAnswer(question1, opponentParticipant, wrongOption, null, false, 3000);
            // Round 2: đối thủ có record nhưng không chọn gì (hết giờ), mình không có record
            BattleAnswer opponentTimeout = buildAnswer(question2, opponentParticipant, null, null, false, 15000);

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            // Trả về đối thủ trước để kiểm tra người gọi API luôn đứng đầu
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(opponentParticipant, myParticipant));
            stubMappers();
            when(questionRepository.findByMatchIdOrderByRoundNo(MATCH_ID)).thenReturn(List.of(question1, question2));
            when(answerRepository.findByMatchIdWithDetails(MATCH_ID))
                    .thenReturn(List.of(myAnswer, opponentAnswer, opponentTimeout));

            BattleMatchDetailResponse response = battleHistoryService.getMatchDetail(USER_ID, MATCH_ID);

            assertThat(response.matchId()).isEqualTo(MATCH_ID);
            assertThat(response.mode()).isEqualTo(BattleMode.MULTIPLE_CHOICE);
            assertThat(response.matchType()).isEqualTo(BattleMatchType.RANDOM);
            assertThat(response.totalRounds()).isEqualTo(2);
            assertThat(response.winnerId()).isEqualTo(USER_ID);
            assertThat(response.startedAt()).isEqualTo(testMatch.getStartedAt());
            assertThat(response.finishedAt()).isEqualTo(testMatch.getFinishedAt());

            // Người gọi API đứng đầu danh sách
            assertThat(response.players()).extracting(PlayerResponse::userId)
                    .containsExactly(USER_ID, OPPONENT_ID);
            assertThat(response.results()).hasSize(2);
            assertThat(response.results().get(0).userId()).isEqualTo(USER_ID);
            assertThat(response.results().get(0).score()).isEqualTo(300);
            assertThat(response.results().get(0).correctCount()).isEqualTo(2);
            assertThat(response.results().get(0).isWinner()).isTrue();
            assertThat(response.results().get(1).userId()).isEqualTo(OPPONENT_ID);
            assertThat(response.results().get(1).isWinner()).isFalse();

            assertThat(response.rounds()).hasSize(2);

            // Round 1
            assertThat(response.rounds().get(0).roundNo()).isEqualTo(1);
            assertThat(response.rounds().get(0).term()).isEqualTo("apple");
            assertThat(response.rounds().get(0).correctAnswer()).isEqualTo("tao");
            BattleRoundAnswerResponse r1Mine = response.rounds().get(0).answers().get(0);
            assertThat(r1Mine.userId()).isEqualTo(USER_ID);
            assertThat(r1Mine.answered()).isTrue();
            assertThat(r1Mine.answerText()).isEqualTo("tao");
            assertThat(r1Mine.isCorrect()).isTrue();
            assertThat(r1Mine.responseTimeMs()).isEqualTo(2000);
            BattleRoundAnswerResponse r1Opponent = response.rounds().get(0).answers().get(1);
            assertThat(r1Opponent.userId()).isEqualTo(OPPONENT_ID);
            assertThat(r1Opponent.answered()).isTrue();
            assertThat(r1Opponent.answerText()).isEqualTo("cam");
            assertThat(r1Opponent.isCorrect()).isFalse();
            assertThat(r1Opponent.responseTimeMs()).isEqualTo(3000);

            // Round 2: cả hai đều coi như không trả lời
            assertThat(response.rounds().get(1).roundNo()).isEqualTo(2);
            assertThat(response.rounds().get(1).answers()).hasSize(2);
            assertThat(response.rounds().get(1).answers()).allSatisfy(a -> {
                assertThat(a.answered()).isFalse();
                assertThat(a.answerText()).isNull();
                assertThat(a.isCorrect()).isFalse();
                assertThat(a.responseTimeMs()).isNull();
            });
        }

        @Test
        @DisplayName("Get match detail successfully with typed answers preferred over selected option")
        void getMatchDetail_Success_TypedAnswer() {
            BattleAnswer myAnswer = buildAnswer(question1, myParticipant, null, "apple", true, 4000);
            BattleAnswer opponentAnswer = buildAnswer(question1, opponentParticipant, null, "aple", false, 6000);

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            stubMappers();
            when(questionRepository.findByMatchIdOrderByRoundNo(MATCH_ID)).thenReturn(List.of(question1));
            when(answerRepository.findByMatchIdWithDetails(MATCH_ID)).thenReturn(List.of(myAnswer, opponentAnswer));

            BattleMatchDetailResponse response = battleHistoryService.getMatchDetail(USER_ID, MATCH_ID);

            assertThat(response.rounds()).hasSize(1);
            assertThat(response.rounds().get(0).answers().get(0).answerText()).isEqualTo("apple");
            assertThat(response.rounds().get(0).answers().get(0).isCorrect()).isTrue();
            assertThat(response.rounds().get(0).answers().get(1).answerText()).isEqualTo("aple");
            assertThat(response.rounds().get(0).answers().get(1).isCorrect()).isFalse();
        }

        @Test
        @DisplayName("Get match detail successfully with draw has null winner id")
        void getMatchDetail_Success_Draw() {
            testMatch.setWinner(null);
            myParticipant.setIsWinner(false);

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            stubMappers();
            when(questionRepository.findByMatchIdOrderByRoundNo(MATCH_ID)).thenReturn(List.of());
            when(answerRepository.findByMatchIdWithDetails(MATCH_ID)).thenReturn(List.of());

            BattleMatchDetailResponse response = battleHistoryService.getMatchDetail(USER_ID, MATCH_ID);

            assertThat(response.winnerId()).isNull();
            assertThat(response.results()).allSatisfy(r -> assertThat(r.isWinner()).isFalse());
            assertThat(response.rounds()).isEmpty();
        }

        @Test
        @DisplayName("Get match detail falls back to question count when total rounds is null")
        void getMatchDetail_Success_TotalRoundsFallback() {
            testMatch.setTotalRounds(null);

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(myParticipant, opponentParticipant));
            stubMappers();
            when(questionRepository.findByMatchIdOrderByRoundNo(MATCH_ID)).thenReturn(List.of(question1, question2));
            when(answerRepository.findByMatchIdWithDetails(MATCH_ID)).thenReturn(List.of());

            BattleMatchDetailResponse response = battleHistoryService.getMatchDetail(USER_ID, MATCH_ID);

            assertThat(response.totalRounds()).isEqualTo(2);
        }

        @Test
        @DisplayName("Get match detail failed: Match not found")
        void getMatchDetail_Fail_MatchNotFound() {
            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> battleHistoryService.getMatchDetail(USER_ID, MATCH_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));

            verify(participantRepository, never()).findByMatchIdWithUser(any());
        }

        @Test
        @DisplayName("Get match detail failed: User is not a participant")
        void getMatchDetail_Fail_NotParticipant() {
            User otherUser = User.builder().id(OTHER_USER_ID).fullName("Other").role(Role.USER).build();
            BattleParticipant otherParticipant = BattleParticipant.builder()
                    .id(13L).match(testMatch).user(otherUser).build();

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(opponentParticipant, otherParticipant));

            assertThatThrownBy(() -> battleHistoryService.getMatchDetail(USER_ID, MATCH_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_PARTICIPANT_NOT_FOUND));

            verify(questionRepository, never()).findByMatchIdOrderByRoundNo(any());
            verify(answerRepository, never()).findByMatchIdWithDetails(any());
        }

        @Test
        @DisplayName("Get match detail failed: Match is not finished")
        void getMatchDetail_Fail_MatchNotFinished() {
            testMatch.setStatus(BattleMatchStatus.IN_PROGRESS);

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(myParticipant, opponentParticipant));

            assertThatThrownBy(() -> battleHistoryService.getMatchDetail(USER_ID, MATCH_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));

            verify(questionRepository, never()).findByMatchIdOrderByRoundNo(any());
        }

        @Test
        @DisplayName("Get match detail failed: Match is cancelled")
        void getMatchDetail_Fail_MatchCancelled() {
            testMatch.setStatus(BattleMatchStatus.CANCELLED);

            when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(testMatch));
            when(participantRepository.findByMatchIdWithUser(MATCH_ID))
                    .thenReturn(List.of(myParticipant, opponentParticipant));

            assertThatThrownBy(() -> battleHistoryService.getMatchDetail(USER_ID, MATCH_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_MATCH_NOT_FOUND));
        }
    }

    // ========================================================================
    // getStats
    // ========================================================================
    @Nested
    @DisplayName("getStats")
    class GetStatsTests {

        @Test
        @DisplayName("Get stats successfully")
        void getStats_Success() {
            when(participantRepository.getStats(USER_ID)).thenReturn(projection(10, 6, 1));

            BattleStatsResponse response = battleHistoryService.getStats(USER_ID);

            assertThat(response.totalMatches()).isEqualTo(10);
            assertThat(response.wins()).isEqualTo(6);
            assertThat(response.draws()).isEqualTo(1);
            assertThat(response.losses()).isEqualTo(3);
            assertThat(response.winRate()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("Get stats rounds win rate to one decimal place")
        void getStats_Success_WinRateRounded() {
            when(participantRepository.getStats(USER_ID)).thenReturn(projection(7, 2, 0));

            BattleStatsResponse response = battleHistoryService.getStats(USER_ID);

            assertThat(response.losses()).isEqualTo(5);
            assertThat(response.winRate()).isEqualTo(28.6);
        }

        @Test
        @DisplayName("Get stats successfully when user has never played")
        void getStats_Success_NoMatches() {
            when(participantRepository.getStats(USER_ID)).thenReturn(projection(0, 0, 0));

            BattleStatsResponse response = battleHistoryService.getStats(USER_ID);

            assertThat(response.totalMatches()).isZero();
            assertThat(response.wins()).isZero();
            assertThat(response.losses()).isZero();
            assertThat(response.draws()).isZero();
            assertThat(response.winRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Get stats successfully when projection is null")
        void getStats_Success_NullProjection() {
            when(participantRepository.getStats(USER_ID)).thenReturn(null);

            BattleStatsResponse response = battleHistoryService.getStats(USER_ID);

            assertThat(response.totalMatches()).isZero();
            assertThat(response.losses()).isZero();
            assertThat(response.winRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Get stats never returns negative losses")
        void getStats_Success_LossesNeverNegative() {
            when(participantRepository.getStats(USER_ID)).thenReturn(projection(2, 2, 1));

            BattleStatsResponse response = battleHistoryService.getStats(USER_ID);

            assertThat(response.losses()).isZero();
        }

        @Test
        @DisplayName("Get stats with all wins has 100 percent win rate")
        void getStats_Success_AllWins() {
            when(participantRepository.getStats(USER_ID)).thenReturn(projection(5, 5, 0));

            BattleStatsResponse response = battleHistoryService.getStats(USER_ID);

            assertThat(response.losses()).isZero();
            assertThat(response.winRate()).isEqualTo(100.0);
        }
    }
}
