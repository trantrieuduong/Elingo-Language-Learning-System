package com.elingo.battle.service.impl;

import com.elingo.battle.dto.response.BattleHistoryItemResponse;
import com.elingo.battle.dto.response.BattleMatchDetailResponse;
import com.elingo.battle.dto.response.BattleRoundAnswerResponse;
import com.elingo.battle.dto.response.BattleRoundDetailResponse;
import com.elingo.battle.dto.response.BattleStatsResponse;
import com.elingo.battle.dto.response.PlayerFinalResultResponse;
import com.elingo.battle.dto.response.PlayerResponse;
import com.elingo.battle.entity.BattleAnswer;
import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.entity.BattleMatchType;
import com.elingo.battle.entity.BattleMode;
import com.elingo.battle.entity.BattleParticipant;
import com.elingo.battle.entity.BattleQuestion;
import com.elingo.battle.entity.BattleResult;
import com.elingo.battle.mapper.BattleMapper;
import com.elingo.battle.repository.BattleAnswerRepository;
import com.elingo.battle.repository.BattleMatchRepository;
import com.elingo.battle.repository.BattleParticipantRepository;
import com.elingo.battle.repository.BattleQuestionRepository;
import com.elingo.battle.service.BattleHistoryService;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j(topic = "BATTLE-HISTORY-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleHistoryServiceImpl implements BattleHistoryService {

    private final BattleParticipantRepository participantRepository;
    private final BattleMatchRepository matchRepository;
    private final BattleQuestionRepository questionRepository;
    private final BattleAnswerRepository answerRepository;
    private final BattleMapper battleMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BattleHistoryItemResponse> getHistory(
            Long userId,
            BattleResult result,
            BattleMode mode,
            BattleMatchType matchType,
            Pageable pageable
    ) {
        int filter = result == null ? 0 : switch (result) {
            case WIN -> 1;
            case LOSE -> 2;
            case DRAW -> 3;
        };

        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<BattleParticipant> minePage = participantRepository.findHistory(userId, filter, mode, matchType, paging);

        List<Long> matchIds = minePage.getContent().stream()
                .map(p -> p.getMatch().getId())
                .toList();

        Map<Long, List<BattleParticipant>> byMatch = matchIds.isEmpty() ? Map.of()
                : participantRepository.findByMatchIds(matchIds).stream()
                .collect(Collectors.groupingBy(p -> p.getMatch().getId()));

        Page<BattleHistoryItemResponse> responsePage = minePage.map(myPart -> {
            BattleMatch match = myPart.getMatch();
            List<BattleParticipant> matchParticipants = byMatch.getOrDefault(match.getId(), List.of());
            BattleParticipant opponentPart = matchParticipants.stream()
                    .filter(p -> !Objects.equals(p.getUser().getId(), userId))
                    .findFirst()
                    .orElse(null);

            PlayerResponse opponentResponse = opponentPart != null
                    ? battleMapper.toPlayerResponse(opponentPart.getUser())
                    : null;

            int myScore = myPart.getScore() != null ? myPart.getScore() : 0;
            int opponentScore = opponentPart != null && opponentPart.getScore() != null
                    ? opponentPart.getScore()
                    : 0;

            BattleResult myResult;
            if (match.getWinner() == null) {
                myResult = BattleResult.DRAW;
            } else {
                myResult = Boolean.TRUE.equals(myPart.getIsWinner()) ? BattleResult.WIN : BattleResult.LOSE;
            }

            return new BattleHistoryItemResponse(
                    match.getId(),
                    match.getMode(),
                    match.getMatchType(),
                    opponentResponse,
                    myScore,
                    opponentScore,
                    myResult,
                    match.getFinishedAt()
            );
        });

        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public BattleMatchDetailResponse getMatchDetail(Long userId, Long matchId) {
        BattleMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new AppException(AppError.BATTLE_MATCH_NOT_FOUND));

        List<BattleParticipant> participants = participantRepository.findByMatchIdWithUser(matchId);
        boolean isParticipant = participants.stream()
                .anyMatch(p -> Objects.equals(p.getUser().getId(), userId));

        if (!isParticipant) {
            throw new AppException(AppError.BATTLE_PARTICIPANT_NOT_FOUND);
        }

        if (match.getStatus() != BattleMatchStatus.FINISHED) {
            throw new AppException(AppError.BATTLE_MATCH_NOT_FOUND);
        }

        // Sắp xếp người tham gia: người gọi API trước, đối thủ sau để nhất quán
        List<BattleParticipant> orderedParticipants = participants.stream()
                .sorted(Comparator.comparing((BattleParticipant p) -> !Objects.equals(p.getUser().getId(), userId))
                        .thenComparing(p -> p.getUser().getId()))
                .toList();

        List<PlayerResponse> players = orderedParticipants.stream()
                .map(p -> battleMapper.toPlayerResponse(p.getUser()))
                .toList();

        List<PlayerFinalResultResponse> results = orderedParticipants.stream()
                .map(p -> new PlayerFinalResultResponse(
                        p.getUser().getId(),
                        p.getScore() != null ? p.getScore() : 0,
                        p.getCorrectCount() != null ? p.getCorrectCount() : 0,
                        Boolean.TRUE.equals(p.getIsWinner())
                ))
                .toList();

        List<BattleQuestion> questions = questionRepository.findByMatchIdOrderByRoundNo(matchId);
        List<BattleAnswer> answers = answerRepository.findByMatchIdWithDetails(matchId);

        Map<Long, List<BattleAnswer>> answersByQuestionId = answers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        List<BattleRoundDetailResponse> rounds = new ArrayList<>();
        for (BattleQuestion question : questions) {
            List<BattleAnswer> questionAnswers = answersByQuestionId.getOrDefault(question.getId(), List.of());
            Map<Long, BattleAnswer> answerByUserId = questionAnswers.stream()
                    .collect(Collectors.toMap(
                            a -> a.getParticipant().getUser().getId(),
                            a -> a,
                            (a1, a2) -> a1
                    ));

            List<BattleRoundAnswerResponse> roundAnswers = new ArrayList<>();
            for (BattleParticipant part : orderedParticipants) {
                Long pUserId = part.getUser().getId();
                BattleAnswer ans = answerByUserId.get(pUserId);

                if (ans == null || (ans.getSelectedOption() == null && ans.getTypedAnswer() == null)) {
                    roundAnswers.add(new BattleRoundAnswerResponse(
                            pUserId,
                            false,
                            null,
                            false,
                            null
                    ));
                } else {
                    String answerText = ans.getTypedAnswer() != null
                            ? ans.getTypedAnswer()
                            : (ans.getSelectedOption() != null ? ans.getSelectedOption().getOptionText() : null);
                    boolean isCorrect = Boolean.TRUE.equals(ans.getIsCorrect());
                    roundAnswers.add(new BattleRoundAnswerResponse(
                            pUserId,
                            true,
                            answerText,
                            isCorrect,
                            ans.getResponseTimeMs()
                    ));
                }
            }

            rounds.add(new BattleRoundDetailResponse(
                    question.getRoundNo() != null ? question.getRoundNo().intValue() : 0,
                    question.getTerm(),
                    question.getCorrectAnswer(),
                    roundAnswers
            ));
        }

        Long winnerId = match.getWinner() != null ? match.getWinner().getId() : null;
        int totalRounds = match.getTotalRounds() != null ? match.getTotalRounds().intValue() : questions.size();

        return new BattleMatchDetailResponse(
                match.getId(),
                match.getMode(),
                match.getMatchType(),
                totalRounds,
                winnerId,
                match.getStartedAt(),
                match.getFinishedAt(),
                players,
                results,
                rounds
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BattleStatsResponse getStats(Long userId) {
        BattleParticipantRepository.BattleStatsProjection projection = participantRepository.getStats(userId);
        long total = projection != null ? projection.getTotal() : 0;
        long wins = projection != null ? projection.getWins() : 0;
        long draws = projection != null ? projection.getDraws() : 0;
        long losses = Math.max(0, total - wins - draws);
        double winRate = total == 0 ? 0.0 : Math.round(wins * 1000.0 / total) / 10.0;

        return new BattleStatsResponse(total, wins, losses, draws, winRate);
    }
}
