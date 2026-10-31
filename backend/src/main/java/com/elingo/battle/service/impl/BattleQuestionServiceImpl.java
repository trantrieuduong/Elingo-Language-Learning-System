package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleQuestion;
import com.elingo.battle.entity.BattleQuestionOption;
import com.elingo.battle.repository.BattleQuestionOptionRepository;
import com.elingo.battle.repository.BattleQuestionRepository;
import com.elingo.battle.service.BattleQuestionService;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.battle.util.BattleTyping;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.entity.Card;
import com.elingo.vocabulary.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j(topic = "BATTLE-QUESTION-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleQuestionServiceImpl implements BattleQuestionService {

    private static final int DISTRACTOR_POOL_SIZE = 40;

    private final CardRepository cardRepository;
    private final BattleQuestionRepository questionRepository;
    private final BattleQuestionOptionRepository optionRepository;
    private final BattleProperties props;

    @Override
    @Transactional
    public List<LiveQuestion> createQuestions(BattleMatch match) {
        return match.getMode() == BattleMode.TYPING
                ? createTypingQuestions(match)
                : createMultipleChoiceQuestions(match);
    }

    /**
     * Multiple-choice question
     */
    private List<LiveQuestion> createMultipleChoiceQuestions(BattleMatch match) {
        int rounds = match.getTotalRounds();

        // 1. Lấy toàn bộ id card hợp lệ
        List<Long> ids = new ArrayList<>(questionRepository.findBattleEligibleCardIds());
        if (ids.size() < Math.max(props.minCardsRequired(), rounds)) {
            throw new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS);
        }
        Collections.shuffle(ids);

        List<Long> questionIds = ids.subList(0, rounds);
        List<Long> poolIds = ids.subList(rounds, Math.min(ids.size(), rounds + DISTRACTOR_POOL_SIZE));

        // 2. Fetch cards (question set + distractor pool)
        List<Long> all = new ArrayList<>(questionIds);
        all.addAll(poolIds);
        Map<Long, Card> cards = cardRepository.findAllById(all).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        // Pool text
        List<String> poolTexts = poolIds.stream()
                .map(id -> cards.get(id).getTranslation().trim())
                .distinct()
                .toList();

        // 3. Xây options text cho mỗi câu
        List<BattleQuestion> questions = new ArrayList<>();
        List<List<String>> optionTextsPerQuestion = new ArrayList<>();

        for (int i = 0; i < rounds; i++) {
            Card card = cards.get(questionIds.get(i));
            String correct = card.getTranslation().trim();

            // Tạo distractors: ưu tiên lấy từ pool, nếu không đủ thì lấy từ các question khác
            List<String> primary = new ArrayList<>(
                    poolTexts.stream().filter(t -> !t.equalsIgnoreCase(correct)).toList());
            List<String> fallback = new ArrayList<>();
            for (int j = 0; j < rounds; j++) {
                if (j == i) continue;
                String t = cards.get(questionIds.get(j)).getTranslation().trim();
                if (!t.equalsIgnoreCase(correct) && !primary.contains(t) && !fallback.contains(t))
                    fallback.add(t);
            }
            Collections.shuffle(primary);
            Collections.shuffle(fallback);
            List<String> distractors = Stream.concat(primary.stream(), fallback.stream()).limit(3).toList();
            if (distractors.size() < 3) throw new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS);

            List<String> options = new ArrayList<>(distractors);
            options.add(correct);
            Collections.shuffle(options);
            optionTextsPerQuestion.add(options);

            questions.add(BattleQuestion.builder()
                    .match(match).card(card).roundNo((short) (i + 1))
                    .term(card.getTerm().trim())
                    .correctAnswer(correct)
                    .timeLimitMs((int) props.questionTimeMs())
                    .build());
        }

        // 4. Lưu questions
        questionRepository.saveAll(questions);

        // 5. Lưu options
        List<BattleQuestionOption> allOptions = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            BattleQuestion q = questions.get(i);
            for (String text : optionTextsPerQuestion.get(i)) {
                allOptions.add(BattleQuestionOption.builder()
                        .question(q).optionText(text)
                        .isCorrect(text.equals(q.getCorrectAnswer()))
                        .build());
            }
        }
        optionRepository.saveAll(allOptions);

        // 6. Tạo LiveQuestion
        List<LiveQuestion> result = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            BattleQuestion q = questions.get(i);
            List<LiveQuestion.LiveOption> opts = allOptions.subList(i * 4, i * 4 + 4).stream()
                    .map(o -> new LiveQuestion.LiveOption(o.getId(), o.getOptionText(), o.getIsCorrect()))
                    .toList();
            result.add(new LiveQuestion(q.getId(), i + 1, q.getTerm(), q.getCorrectAnswer(), q.getTimeLimitMs(), opts));
        }

        log.info("MC questions created matchId={} rounds={}", match.getId(), rounds);
        return result;
    }

    /**
     * Typing question
     */
    private List<LiveQuestion> createTypingQuestions(BattleMatch match) {
        int rounds = match.getTotalRounds();

        // 1. Lấy id card hợp lệ
        List<Long> ids = new ArrayList<>(questionRepository.findBattleTypingEligibleCardIds());
        if (ids.size() < Math.max(props.minCardsRequired(), rounds)) {
            throw new AppException(AppError.BATTLE_NOT_ENOUGH_CARDS);
        }
        Collections.shuffle(ids);
        List<Long> picked = ids.subList(0, rounds);

        // 2. Fetch cards
        Map<Long, Card> cards = cardRepository.findAllById(picked).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        // 3. Tạo questions
        List<BattleQuestion> questions = new ArrayList<>();
        for (int i = 0; i < rounds; i++) {
            Card card = cards.get(picked.get(i));
            questions.add(BattleQuestion.builder()
                    .match(match).card(card).roundNo((short) (i + 1))
                    .term(card.getTranslation().trim())
                    .correctAnswer(BattleTyping.normalize(card.getTerm()))
                    .timeLimitMs((int) props.questionTimeMs())
                    .build());
        }
        // 4. Lưu questions
        questionRepository.saveAll(questions);

        // 5. Tạo LiveQuestion
        List<LiveQuestion> result = new ArrayList<>();
        for (BattleQuestion q : questions) {
            result.add(new LiveQuestion(q.getId(), q.getRoundNo(), q.getTerm(),
                    q.getCorrectAnswer(), q.getTimeLimitMs(), List.of()));
        }
        log.info("Typing questions created matchId={} rounds={}", match.getId(), rounds);
        return result;
    }
}
