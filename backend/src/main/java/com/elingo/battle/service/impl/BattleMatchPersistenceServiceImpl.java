package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.request.RoundAnswerRecord;
import com.elingo.battle.dto.response.CreatedMatchResult;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.*;
import com.elingo.battle.mapper.BattleMapper;
import com.elingo.battle.repository.*;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.service.BattleQuestionService;
import com.elingo.battle.state.LiveQuestion;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "BATTLE-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleMatchPersistenceServiceImpl implements BattleMatchPersistenceService {

    private final BattleMatchRepository matchRepository;
    private final BattleParticipantRepository participantRepository;
    private final BattleAnswerRepository answerRepository;
    private final BattleQuestionRepository questionRepository;
    private final BattleQuestionOptionRepository optionRepository;
    private final UserRepository userRepository;
    private final BattleMapper battleMapper;
    private final BattleProperties battleProperties;
    private final BattleQuestionService questionService;

    @Override
    @Transactional
    public CreatedMatchResult persistNewRandomMatch(Long userAId, Long userBId, BattleMode mode) {
        User userA = userRepository.findById(userAId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        User userB = userRepository.findById(userBId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        BattleMatch match = BattleMatch.builder()
                .mode(mode)
                .matchType(BattleMatchType.RANDOM)
                .status(BattleMatchStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .totalRounds((short) battleProperties.totalRounds())
                .build();
        matchRepository.save(match);

        BattleParticipant partA = BattleParticipant.builder().match(match).user(userA).build();
        BattleParticipant partB = BattleParticipant.builder().match(match).user(userB).build();
        participantRepository.saveAll(List.of(partA, partB));

        // Sinh câu hỏi trong cùng transaction
        List<LiveQuestion> questions = questionService.createQuestions(match);

        Map<Long, Long> participantIds = Map.of(userAId, partA.getId(), userBId, partB.getId());

        log.info("Match persisted matchId={} mode={} userAId={} userBId={}", match.getId(), mode, userAId, userBId);

        PlayerResponse playerA = battleMapper.toPlayerResponse(userA);
        PlayerResponse playerB = battleMapper.toPlayerResponse(userB);

        return new CreatedMatchResult(match.getId(), mode, userAId, userBId,
                List.of(playerA, playerB), participantIds, questions);
    }

    @Override
    @Transactional
    public void updateMatchStatus(Long matchId, BattleMatchStatus status) {
        matchRepository.findById(matchId).ifPresent(m -> {
            m.setStatus(status);
            matchRepository.save(m);
        });
    }

    @Override
    @Transactional
    public void saveRoundAnswers(List<RoundAnswerRecord> records) {
        List<BattleAnswer> answers = new java.util.ArrayList<>();
        for (RoundAnswerRecord r : records) {
            answers.add(BattleAnswer.builder()
                    .question(questionRepository.getReferenceById(r.questionId()))
                    .participant(participantRepository.getReferenceById(r.participantId()))
                    .selectedOption(r.selectedOptionId() == null ? null
                            : optionRepository.getReferenceById(r.selectedOptionId()))
                    .typedAnswer(r.typedAnswer())
                    .isCorrect(r.correct())
                    .responseTimeMs(r.responseTimeMs())
                    .build());
        }
        answerRepository.saveAll(answers);
    }

    @Override
    @Transactional
    public void finishMatch(Long matchId, Long winnerUserId,
                            Map<Long, Integer> scores, Map<Long, Integer> correctCounts) {
        BattleMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new AppException(AppError.BATTLE_MATCH_NOT_FOUND));
        for (BattleParticipant p : participantRepository.findByMatchIdWithUser(matchId)) {
            Long uid = p.getUser().getId();
            p.setScore(scores.getOrDefault(uid, 0));
            p.setCorrectCount(correctCounts.getOrDefault(uid, 0));
            p.setIsWinner(uid.equals(winnerUserId));
        }
        match.setStatus(BattleMatchStatus.FINISHED);
        match.setFinishedAt(LocalDateTime.now());
        if (winnerUserId != null) match.setWinner(userRepository.getReferenceById(winnerUserId));
    }
}
