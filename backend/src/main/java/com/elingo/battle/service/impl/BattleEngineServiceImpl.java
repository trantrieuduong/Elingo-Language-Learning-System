package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.request.RoundAnswerRecord;
import com.elingo.battle.dto.response.*;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.service.BattleMatchPersistenceService;
import com.elingo.battle.service.BattleXpService;
import com.elingo.battle.state.*;
import com.elingo.battle.util.BattleScoring;
import com.elingo.battle.util.BattleTyping;
import com.elingo.battle.websocket.BattleEventPublisher;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j(topic = "BATTLE-ENGINE-SERVICE")
@Service
@RequiredArgsConstructor
public class BattleEngineServiceImpl implements BattleEngineService {

    private final BattleLiveRegistry registry;
    private final BattleEventPublisher publisher;
    private final BattleMatchPersistenceService persistence;
    private final BattleProperties props;
    private final ThreadPoolTaskScheduler battleScheduler;
    private final BattleXpService xpService;

    @Override
    public void start(LiveMatch m) {
        scheduleNext(m, props.countdownMs(), () -> startRound(m, 1));
    }

    @Override
    public void submitAnswer(Long userId, BattleAnswerRequest req) {
        LiveMatch m = registry.getByUser(userId);
        if (m == null || !m.getMatchId().equals(req.matchId()))
            throw new AppException(AppError.BATTLE_PARTICIPANT_NOT_FOUND);

        m.lock();
        try {
            LiveRound r = m.getCurrentRound();
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS || r == null || r.isEnded()
                    || !r.getQuestion().questionId().equals(req.questionId()))
                throw new AppException(AppError.BATTLE_QUESTION_NOT_FOUND);
            if (r.hasAnswered(userId))
                throw new AppException(AppError.BATTLE_QUESTION_ALREADY_ANSWERED);

            long now = System.currentTimeMillis();
            if (now > r.getDeadlineAt() + props.answerGraceMs())
                throw new AppException(AppError.BATTLE_QUESTION_TIMEOUT);

            LiveQuestion q = r.getQuestion();
            long responseMs = Math.min(Math.max(0, now - r.getStartedAt()), q.timeLimitMs());
            boolean correct;
            Long optionId = null;
            String typed = null;

            if (q.isTyping()) {
                typed = req.typedAnswer();
                if (typed == null || typed.isBlank() || typed.length() > BattleTyping.MAX_ANSWER_LENGTH)
                    throw new AppException(AppError.BATTLE_ANSWER_INVALID);
                typed = typed.trim();
                correct = BattleTyping.normalize(typed).equals(q.correctAnswer());
            } else {
                optionId = req.selectedOptionId();
                if (optionId == null || !q.hasOption(optionId))
                    throw new AppException(AppError.BATTLE_ANSWER_INVALID);
                correct = q.isCorrectOption(optionId);
            }

            int points = BattleScoring.points(correct, responseMs, q.timeLimitMs());
            r.putAnswer(new LiveAnswer(userId, optionId, typed, correct, (int) responseMs, points));

            // Báo đối thủ biết người kia đã trả lời
            publisher.sendToUser(m.opponentOf(userId), "match:opponent-answered",
                    new OpponentAnsweredResponse(m.getMatchId(), r.getRoundNo()));

            // Cả hai đã trả lời -> kết thúc vòng ngay
            if (r.answerCount() == 2) {
                cancelPending(m);
                endRound(m, r.getRoundNo());
            }
        } finally {
            m.unlock();
        }
    }

    /**
     * Game flow
     */
    private void startRound(LiveMatch m, int roundNo) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            m.setLastRoundResult(null); // Bắt đầu vòng mới -> reset kết quả vòng cũ
            LiveQuestion q = m.getQuestions().get(roundNo - 1);
            long now = System.currentTimeMillis();
            LiveRound r = new LiveRound(roundNo, q, now, now + q.timeLimitMs());
            m.setCurrentRound(r);

            MatchQuestionResponse payload = questionPayload(m, r, now);
            publisher.sendToUser(m.getUserAId(), "match:question", payload);
            publisher.sendToUser(m.getUserBId(), "match:question", payload);

            // Hết giờ câu -> endRound
            scheduleNext(m, q.timeLimitMs(), () -> endRound(m, roundNo));
        } finally {
            m.unlock();
        }
    }

    private MatchQuestionResponse questionPayload(LiveMatch m, LiveRound r, long serverNow) {
        LiveQuestion q = r.getQuestion();
        boolean typing = q.isTyping();
        List<OptionResponse> options = typing ? null
                : q.options().stream().map(o -> new OptionResponse(o.id(), o.text())).toList();
        Integer answerLength = typing ? q.correctAnswer().length() : null;
        List<Integer> pattern = typing ? BattleTyping.pattern(q.correctAnswer()) : null;
        String firstChar = typing ? BattleTyping.firstChar(q.correctAnswer()) : null;

        return new MatchQuestionResponse(
                m.getMatchId(), q.questionId(), r.getRoundNo(), m.getTotalRounds(), q.term(),
                options, m.getMode(), q.timeLimitMs(), serverNow, r.getDeadlineAt(),
                answerLength, pattern, firstChar);
    }

    private void endRound(LiveMatch m, int roundNo) {
        m.lock();
        try {
            LiveRound r = m.getCurrentRound();
            // Idempotent: nếu đã ended hoặc vòng khác thì bỏ qua
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS
                    || r == null || r.isEnded() || r.getRoundNo() != roundNo) return;
            r.markEnded();
            cancelPending(m);

            LiveQuestion q = r.getQuestion();
            List<PlayerRoundResultResponse> results = new ArrayList<>();
            List<RoundAnswerRecord> toSave = new ArrayList<>();

            for (Long uid : List.of(m.getUserAId(), m.getUserBId())) {
                LiveAnswer a = r.getAnswer(uid);
                boolean answered = a != null;
                boolean correct = answered && a.correct();
                int points = answered ? a.points() : 0;
                int rt = answered ? a.responseTimeMs() : q.timeLimitMs();
                m.addScore(uid, points, correct);
                results.add(new PlayerRoundResultResponse(uid, answered, correct, points,
                        m.getScore(uid), m.getCorrectCount(uid), rt));
                toSave.add(new RoundAnswerRecord(q.questionId(), m.getParticipantId(uid),
                        answered ? a.selectedOptionId() : null,
                        answered ? a.typedAnswer() : null,
                        correct, rt));
            }

            // Lưu đáp án 2 người chơi
            persistence.saveRoundAnswers(toSave);

            RoundResultResponse roundPayload =
                    new RoundResultResponse(m.getMatchId(), roundNo, q.correctAnswer(), results);
            m.setLastRoundResult(roundPayload);
            publisher.sendToUser(m.getUserAId(), "match:round-result", roundPayload);
            publisher.sendToUser(m.getUserBId(), "match:round-result", roundPayload);

            scheduleNext(m, props.roundGapMs(), () -> afterRound(m, roundNo));
        } finally {
            m.unlock();
        }
    }

    private void afterRound(LiveMatch m, int roundNo) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            if (roundNo >= m.getTotalRounds()) finish(m);
            else startRound(m, roundNo + 1);
        } finally {
            m.unlock();
        }
    }

    private void finish(LiveMatch m) {
        int sa = m.getScore(m.getUserAId());
        int sb = m.getScore(m.getUserBId());
        Long winnerId = sa > sb ? m.getUserAId() : (sb > sa ? m.getUserBId() : null); // null = hòa
        conclude(m, winnerId, "COMPLETED");
    }

    @Override
    public void forfeit(LiveMatch m, Long loserId) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            cancelPending(m); // dừng vòng đang chạy
            conclude(m, m.opponentOf(loserId), "FORFEIT");
        } finally {
            m.unlock();
        }
    }

    private void conclude(LiveMatch m, Long winnerId, String reason) {
        persistence.finishMatch(m.getMatchId(), winnerId, m.getScores(), m.getCorrectCounts());
        m.setStatus(BattleMatchStatus.FINISHED);
        m.cancelAllGraces();

        int sa = m.getScore(m.getUserAId());
        int sb = m.getScore(m.getUserBId());
        List<PlayerFinalResultResponse> players = List.of(
                new PlayerFinalResultResponse(m.getUserAId(), sa,
                        m.getCorrectCount(m.getUserAId()), m.getUserAId().equals(winnerId)),
                new PlayerFinalResultResponse(m.getUserBId(), sb,
                        m.getCorrectCount(m.getUserBId()), m.getUserBId().equals(winnerId)));

        MatchFinishedResponse payload =
                new MatchFinishedResponse(m.getMatchId(), winnerId, reason, players);
        publisher.sendToUser(m.getUserAId(), "match:finished", payload);
        publisher.sendToUser(m.getUserBId(), "match:finished", payload);
        registry.remove(m);

        log.info("Match finished matchId={} winnerId={} scoreA={} scoreB={} reason={}",
                m.getMatchId(), winnerId, sa, sb, reason);

        try {
            xpService.awardMatchXp(m, winnerId, reason);
        } catch (Exception ex) {
            log.warn("Award battle XP failed matchId={}: {}", m.getMatchId(), ex.getMessage());
        }
    }

    /**
     * Private helper methods
     */
    private void scheduleNext(LiveMatch m, long delayMs, Runnable task) {
        m.setPendingFuture(battleScheduler.schedule(
                () -> runSafely(m, task),
                Instant.now().plusMillis(delayMs)));
    }

    private void cancelPending(LiveMatch m) {
        ScheduledFuture<?> f = m.getPendingFuture();
        if (f != null) f.cancel(false);
    }

    // Task chạy trên thread scheduler: lỗi bất ngờ -> hủy trận, không để user kẹt trong trận đấu
    private void runSafely(LiveMatch m, Runnable task) {
        try {
            task.run();
        } catch (Exception ex) {
            log.error("Battle engine error matchId={}", m.getMatchId(), ex);
            cancel(m, "INTERNAL_ERROR");
        }
    }

    @Override
    public void cancel(LiveMatch m, String reason) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            m.setStatus(BattleMatchStatus.CANCELLED);
            cancelPending(m);
            m.cancelAllGraces();
            try {
                persistence.updateMatchStatus(m.getMatchId(), BattleMatchStatus.CANCELLED);
            } catch (Exception ex) {
                log.error("Cannot mark match cancelled matchId={}", m.getMatchId(), ex);
            }
            MatchCancelledResponse payload = new MatchCancelledResponse(m.getMatchId(), reason);
            publisher.sendToUser(m.getUserAId(), "match:cancelled", payload);
            publisher.sendToUser(m.getUserBId(), "match:cancelled", payload);
            registry.remove(m);
        } finally {
            m.unlock();
        }
    }

    @Override
    public MatchRejoinedResponse snapshot(LiveMatch m, Long userId) {
        m.lock();
        try {
            LiveRound r = m.getCurrentRound();
            boolean inRound = r != null && !r.isEnded();
            MatchQuestionResponse q = inRound ? questionPayload(m, r, System.currentTimeMillis()) : null;
            List<PlayerScoreResponse> scores = List.of(
                    new PlayerScoreResponse(m.getUserAId(), m.getScore(m.getUserAId()), m.getCorrectCount(m.getUserAId())),
                    new PlayerScoreResponse(m.getUserBId(), m.getScore(m.getUserBId()), m.getCorrectCount(m.getUserBId())));
            return new MatchRejoinedResponse(new MatchSnapshotResponse(
                    m.getMatchId(), m.getMode(), m.getMatchType(), m.getTotalRounds(), m.getPlayers(), scores,
                    r == null ? 0 : r.getRoundNo(), q, inRound && r.hasAnswered(userId), m.getLastRoundResult()));
        } finally {
            m.unlock();
        }
    }
}

