package com.elingo.battle.service;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleQuestion;
import com.elingo.battle.entity.BattleQuestionOption;
import com.elingo.battle.repository.BattleQuestionOptionRepository;
import com.elingo.battle.repository.BattleQuestionRepository;
import com.elingo.battle.service.impl.BattleQuestionServiceImpl;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class BattleQuestionServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BattleQuestionRepository questionRepository;

    @Mock
    private BattleQuestionOptionRepository optionRepository;

    @Captor
    private ArgumentCaptor<List<BattleQuestion>> questionsCaptor;

    @Captor
    private ArgumentCaptor<List<BattleQuestionOption>> optionsCaptor;

    @InjectMocks
    private BattleQuestionServiceImpl questionService;

    private static final Long MATCH_ID = 100L;
    private static final int ROUNDS = 5;

    private static final BattleProperties PROPS = new BattleProperties(
            10, 15000L, 3000L, 2000L, 30000L, 10000L, 600000L, 3, 20, 1000L);

    private final Map<Long, Card> cardsById = new HashMap<>();

    @BeforeEach
    void setUp() {
        // BattleProperties là record nên không @Mock được -> gán trực tiếp
        ReflectionTestUtils.setField(questionService, "props", PROPS);
    }

    private BattleMatch buildMatch(BattleMode mode, int rounds) {
        return BattleMatch.builder()
                .id(MATCH_ID)
                .mode(mode)
                .matchType(BattleMatchType.RANDOM)
                .status(BattleMatchStatus.IN_PROGRESS)
                .totalRounds((short) rounds)
                .build();
    }

    /** Tạo card có term/translation chứa khoảng trắng thừa để kiểm tra việc trim/normalize */
    private List<Long> prepareCards(int count) {
        List<Long> ids = LongStream.rangeClosed(1, count).boxed().collect(Collectors.toList());
        for (Long id : ids) {
            cardsById.put(id, Card.builder()
                    .id(id)
                    .term("  Mixed   Case" + id + "  ")
                    .translation("  meaning" + id + " ")
                    .build());
        }
        return ids;
    }

    private void stubFindAllById() {
        when(cardRepository.findAllById(any())).thenAnswer(inv -> {
            Iterable<Long> ids = inv.getArgument(0);
            List<Card> result = new ArrayList<>();
            ids.forEach(id -> result.add(cardsById.get(id)));
            return result;
        });
    }

    private void stubSaveQuestions() {
        when(questionRepository.saveAll(any())).thenAnswer(inv -> {
            List<BattleQuestion> list = inv.getArgument(0);
            list.forEach(q -> q.setId(1000L + q.getRoundNo()));
            return list;
        });
    }

    private void stubSaveOptions() {
        AtomicLong sequence = new AtomicLong(5000L);
        when(optionRepository.saveAll(any())).thenAnswer(inv -> {
            List<BattleQuestionOption> list = inv.getArgument(0);
            list.forEach(o -> o.setId(sequence.incrementAndGet()));
            return list;
        });
    }

    // ========================================================================
    // createQuestions - multiple choice
    // ========================================================================
    @Nested
    @DisplayName("createQuestions - multiple choice")
    class MultipleChoiceTests {

        @Test
        @DisplayName("Create multiple choice questions successfully")
        void createQuestions_MultipleChoice_Success() {
            when(questionRepository.findBattleEligibleCardIds()).thenReturn(prepareCards(30));
            stubFindAllById();
            stubSaveQuestions();
            stubSaveOptions();

            List<LiveQuestion> result = questionService.createQuestions(buildMatch(BattleMode.MULTIPLE_CHOICE, ROUNDS));

            assertThat(result).hasSize(ROUNDS);
            for (int i = 0; i < ROUNDS; i++) {
                LiveQuestion q = result.get(i);
                String cardNo = q.term().replace("Mixed   Case", "");
                assertThat(q.roundNo()).isEqualTo(i + 1);
                assertThat(q.questionId()).isEqualTo(1000L + (i + 1));
                assertThat(q.timeLimitMs()).isEqualTo((int) PROPS.questionTimeMs());
                // term và đáp án đã được trim
                assertThat(q.term()).startsWith("Mixed").doesNotEndWith(" ");
                assertThat(q.correctAnswer()).isEqualTo("meaning" + cardNo);
                assertThat(q.isTyping()).isFalse();

                assertThat(q.options()).hasSize(4);
                assertThat(q.options()).extracting(LiveQuestion.LiveOption::text).doesNotHaveDuplicates();
                assertThat(q.options()).extracting(LiveQuestion.LiveOption::id).doesNotContainNull();
                assertThat(q.options().stream().filter(LiveQuestion.LiveOption::correct).toList()).hasSize(1);
                LiveQuestion.LiveOption correctOption = q.options().stream()
                        .filter(LiveQuestion.LiveOption::correct).findFirst().orElseThrow();
                assertThat(correctOption.text()).isEqualTo(q.correctAnswer());
            }
            // Các câu hỏi dùng card khác nhau
            assertThat(result).extracting(LiveQuestion::term).doesNotHaveDuplicates();

            verify(questionRepository, never()).findBattleTypingEligibleCardIds();
        }

        @Test
        @DisplayName("Create multiple choice questions saves questions and options")
        void createQuestions_MultipleChoice_SavesEntities() {
            BattleMatch match = buildMatch(BattleMode.MULTIPLE_CHOICE, ROUNDS);
            when(questionRepository.findBattleEligibleCardIds()).thenReturn(prepareCards(30));
            stubFindAllById();
            stubSaveQuestions();
            stubSaveOptions();

            questionService.createQuestions(match);

            verify(questionRepository).saveAll(questionsCaptor.capture());
            List<BattleQuestion> savedQuestions = questionsCaptor.getValue();
            assertThat(savedQuestions).hasSize(ROUNDS);
            for (int i = 0; i < ROUNDS; i++) {
                BattleQuestion q = savedQuestions.get(i);
                assertThat(q.getMatch()).isSameAs(match);
                assertThat(q.getRoundNo()).isEqualTo((short) (i + 1));
                assertThat(q.getTimeLimitMs()).isEqualTo((int) PROPS.questionTimeMs());
                assertThat(q.getCard()).isNotNull();
            }

            verify(optionRepository).saveAll(optionsCaptor.capture());
            List<BattleQuestionOption> savedOptions = optionsCaptor.getValue();
            assertThat(savedOptions).hasSize(ROUNDS * 4);
            for (BattleQuestion q : savedQuestions) {
                List<BattleQuestionOption> optionsOfQuestion = savedOptions.stream()
                        .filter(o -> o.getQuestion() == q).toList();
                assertThat(optionsOfQuestion).hasSize(4);
                assertThat(optionsOfQuestion.stream().filter(BattleQuestionOption::getIsCorrect).toList()).hasSize(1);
                assertThat(optionsOfQuestion.stream()
                        .filter(BattleQuestionOption::getIsCorrect)
                        .findFirst().orElseThrow().getOptionText()).isEqualTo(q.getCorrectAnswer());
            }
        }

        @Test
        @DisplayName("Create multiple choice questions failed: Not enough eligible cards")
        void createQuestions_MultipleChoice_Fail_NotEnoughCards() {
            when(questionRepository.findBattleEligibleCardIds()).thenReturn(prepareCards(PROPS.minCardsRequired() - 1));

            assertThatThrownBy(() -> questionService.createQuestions(buildMatch(BattleMode.MULTIPLE_CHOICE, ROUNDS)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_NOT_ENOUGH_CARDS));

            verify(cardRepository, never()).findAllById(any());
            verify(questionRepository, never()).saveAll(any());
            verifyNoInteractions(optionRepository);
        }

        @Test
        @DisplayName("Create multiple choice questions failed: Fewer cards than rounds")
        void createQuestions_MultipleChoice_Fail_FewerCardsThanRounds() {
            when(questionRepository.findBattleEligibleCardIds()).thenReturn(prepareCards(22));

            assertThatThrownBy(() -> questionService.createQuestions(buildMatch(BattleMode.MULTIPLE_CHOICE, 25)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_NOT_ENOUGH_CARDS));

            verify(questionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Create multiple choice questions failed: Not enough distinct distractors")
        void createQuestions_MultipleChoice_Fail_NotEnoughDistractors() {
            List<Long> ids = prepareCards(PROPS.minCardsRequired());
            // Mọi card có cùng bản dịch -> không thể tạo 3 đáp án nhiễu khác đáp án đúng
            cardsById.values().forEach(c -> c.setTranslation("same meaning"));
            when(questionRepository.findBattleEligibleCardIds()).thenReturn(ids);
            stubFindAllById();

            assertThatThrownBy(() -> questionService.createQuestions(buildMatch(BattleMode.MULTIPLE_CHOICE, ROUNDS)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_NOT_ENOUGH_CARDS));

            verify(questionRepository, never()).saveAll(any());
            verifyNoInteractions(optionRepository);
        }
    }

    // ========================================================================
    // createQuestions - typing
    // ========================================================================
    @Nested
    @DisplayName("createQuestions - typing")
    class TypingTests {

        @Test
        @DisplayName("Create typing questions successfully")
        void createQuestions_Typing_Success() {
            when(questionRepository.findBattleTypingEligibleCardIds()).thenReturn(prepareCards(30));
            stubFindAllById();
            stubSaveQuestions();

            List<LiveQuestion> result = questionService.createQuestions(buildMatch(BattleMode.TYPING, ROUNDS));

            assertThat(result).hasSize(ROUNDS);
            for (int i = 0; i < ROUNDS; i++) {
                LiveQuestion q = result.get(i);
                String cardNo = q.correctAnswer().replace("mixed case", "");
                assertThat(q.roundNo()).isEqualTo(i + 1);
                assertThat(q.questionId()).isEqualTo(1000L + (i + 1));
                assertThat(q.timeLimitMs()).isEqualTo((int) PROPS.questionTimeMs());
                // Đề bài là nghĩa (đã trim), đáp án là term đã normalize (lowercase, gộp khoảng trắng)
                assertThat(q.term()).isEqualTo("meaning" + cardNo);
                assertThat(q.correctAnswer()).isEqualTo("mixed case" + cardNo);
                assertThat(q.isTyping()).isTrue();
                assertThat(q.options()).isEmpty();
            }
            assertThat(result).extracting(LiveQuestion::questionId).doesNotHaveDuplicates();

            verify(questionRepository, never()).findBattleEligibleCardIds();
            verifyNoInteractions(optionRepository);
        }

        @Test
        @DisplayName("Create typing questions saves questions with match, round number and time limit")
        void createQuestions_Typing_SavesQuestions() {
            BattleMatch match = buildMatch(BattleMode.TYPING, ROUNDS);
            when(questionRepository.findBattleTypingEligibleCardIds()).thenReturn(prepareCards(30));
            stubFindAllById();
            stubSaveQuestions();

            questionService.createQuestions(match);

            verify(questionRepository).saveAll(questionsCaptor.capture());
            List<BattleQuestion> savedQuestions = questionsCaptor.getValue();
            assertThat(savedQuestions).hasSize(ROUNDS);
            for (int i = 0; i < ROUNDS; i++) {
                BattleQuestion q = savedQuestions.get(i);
                assertThat(q.getMatch()).isSameAs(match);
                assertThat(q.getRoundNo()).isEqualTo((short) (i + 1));
                assertThat(q.getTimeLimitMs()).isEqualTo((int) PROPS.questionTimeMs());
                assertThat(q.getCard()).isNotNull();
            }
        }

        @Test
        @DisplayName("Create typing questions failed: Not enough eligible cards")
        void createQuestions_Typing_Fail_NotEnoughCards() {
            when(questionRepository.findBattleTypingEligibleCardIds()).thenReturn(prepareCards(PROPS.minCardsRequired() - 1));

            assertThatThrownBy(() -> questionService.createQuestions(buildMatch(BattleMode.TYPING, ROUNDS)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.BATTLE_NOT_ENOUGH_CARDS));

            verify(cardRepository, never()).findAllById(any());
            verify(questionRepository, never()).saveAll(any());
        }
    }
}
