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
            Pageable pageable) {
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
                    match.getFinishedAt());
        });

        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public BattleMatchDetailResponse getMatchDetail(Long userId, Long matchId) {
        BattleMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new AppException(AppError.BATTLE_MATCH_NOT_FOUND));

        List<BattleParticipant> participants = participantRepository.findByMatchIdWithUser(matchId);
        validateMatchDetailAccess(match, participants, userId);

        // Sắp xếp người tham gia: người gọi API trước, đối thủ sau để nhất quán
        List<BattleParticipant> orderedParticipants = orderParticipants(participants, userId);

        List<BattleQuestion> questions = questionRepository.findByMatchIdOrderByRoundNo(matchId);
        List<BattleAnswer> answers = answerRepository.findByMatchIdWithDetails(matchId);

        List<BattleRoundDetailResponse> rounds = buildRoundDetails(questions, answers, orderedParticipants);
        return buildMatchDetailResponse(match, questions, orderedParticipants, rounds);
    }

    private void validateMatchDetailAccess(BattleMatch match, List<BattleParticipant> participants, Long userId) {
        boolean isParticipant = participants.stream().anyMatch(p -> Objects.equals(p.getUser().getId(), userId));

        if (!isParticipant) {
            throw new AppException(AppError.BATTLE_PARTICIPANT_NOT_FOUND);
        }

        if (match.getStatus() != BattleMatchStatus.FINISHED) {
            throw new AppException(AppError.BATTLE_MATCH_NOT_FOUND);
        }
    }

    private List<BattleParticipant> orderParticipants(List<BattleParticipant> participants, Long userId) {
        return participants.stream()
                .sorted(Comparator.comparing((BattleParticipant p) -> !Objects.equals(p.getUser().getId(), userId))
                        .thenComparing(p -> p.getUser().getId()))
                .toList();
    }

    private List<BattleRoundDetailResponse> buildRoundDetails(List<BattleQuestion> questions,
                                                              List<BattleAnswer> answers,
                                                              List<BattleParticipant> participants) {
        Map<Long, List<BattleAnswer>> answersByQuestionId = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));

        return questions.stream()
                .map(question -> buildRoundDetail(question, answersByQuestionId, participants))
                .toList();
    }

    private BattleRoundDetailResponse buildRoundDetail(BattleQuestion question,
                                                       Map<Long, List<BattleAnswer>> answersByQuestionId,
                                                       List<BattleParticipant> participants) {
        Map<Long, BattleAnswer> answerByUserId = answersByQuestionId.getOrDefault(question.getId(), List.of()).stream()
                .collect(Collectors.toMap(answer -> answer.getParticipant().getUser().getId(), answer -> answer,
                        (first, ignored) -> first));

        List<BattleRoundAnswerResponse> answers = participants.stream()
                .map(participant -> buildRoundAnswer(participant, answerByUserId.get(participant.getUser().getId())))
                .toList();

        return new BattleRoundDetailResponse(question.getRoundNo() != null ? question.getRoundNo().intValue() : 0,
                question.getTerm(), question.getCorrectAnswer(), answers);
    }

    private BattleRoundAnswerResponse buildRoundAnswer(BattleParticipant participant, BattleAnswer answer) {
        Long participantUserId = participant.getUser().getId();

        if (answer == null || (answer.getSelectedOption() == null && answer.getTypedAnswer() == null)) {
            return new BattleRoundAnswerResponse(participantUserId, false, null, false, null);
        }

        String answerText = answer.getTypedAnswer() != null
                ? answer.getTypedAnswer()
                : answer.getSelectedOption().getOptionText();

        return new BattleRoundAnswerResponse(participantUserId, true, answerText,
                Boolean.TRUE.equals(answer.getIsCorrect()), answer.getResponseTimeMs());
    }

    private BattleMatchDetailResponse buildMatchDetailResponse(BattleMatch match, List<BattleQuestion> questions,
                                                               List<BattleParticipant> participants,
                                                               List<BattleRoundDetailResponse> rounds) {
        List<PlayerResponse> players = participants.stream().map(p -> battleMapper.toPlayerResponse(p.getUser()))
                .toList();
        List<PlayerFinalResultResponse> results = participants.stream().map(this::toPlayerFinalResult).toList();

        int totalRounds = match.getTotalRounds() != null ? match.getTotalRounds().intValue() : questions.size();
        Long winnerId = match.getWinner() != null ? match.getWinner().getId() : null;

        return new BattleMatchDetailResponse(match.getId(), match.getMode(), match.getMatchType(), totalRounds,
                winnerId, match.getStartedAt(), match.getFinishedAt(), players, results, rounds);
    }

    private PlayerFinalResultResponse toPlayerFinalResult(BattleParticipant participant) {
        return new PlayerFinalResultResponse(participant.getUser().getId(),
                participant.getScore() != null ? participant.getScore() : 0,
                participant.getCorrectCount() != null ? participant.getCorrectCount() : 0,
                Boolean.TRUE.equals(participant.getIsWinner()));
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
