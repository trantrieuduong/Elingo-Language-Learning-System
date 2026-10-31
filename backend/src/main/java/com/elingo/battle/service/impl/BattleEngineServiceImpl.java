package com.elingo.battle.service.impl;

import com.elingo.battle.config.BattleProperties;
import com.elingo.battle.dto.request.BattleAnswerRequest;
import com.elingo.battle.dto.request.RoundAnswerRecord;
import com.elingo.battle.dto.response.*;
import com.elingo.battle.entity.BattleMatchStatus;
import com.elingo.battle.service.BattleEngineService;
import com.elingo.battle.service.BattleMatchPersistenceService;
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
            LiveQuestion q = m.getQuestions().get(roundNo - 1);
            long now = System.currentTimeMillis();
            LiveRound r = new LiveRound(roundNo, q, now, now + q.timeLimitMs());
            m.setCurrentRound(r);

            boolean typing = q.isTyping();
            List<OptionResponse> options = typing ? null
                    : q.options().stream().map(o -> new OptionResponse(o.id(), o.text())).toList();
            Integer answerLength = typing ? q.correctAnswer().length() : null;
            List<Integer> pattern = typing ? BattleTyping.pattern(q.correctAnswer()) : null;
            String firstChar = typing ? BattleTyping.firstChar(q.correctAnswer()) : null;

            MatchQuestionResponse payload = new MatchQuestionResponse(
                    m.getMatchId(), q.questionId(), roundNo, m.getTotalRounds(), q.term(),
                    options, m.getMode(), q.timeLimitMs(), now, r.getDeadlineAt(),
                    answerLength, pattern, firstChar);
            publisher.sendToUser(m.getUserAId(), "match:question", payload);
            publisher.sendToUser(m.getUserBId(), "match:question", payload);

            // Hết giờ câu -> endRound
            scheduleNext(m, q.timeLimitMs(), () -> endRound(m, roundNo));
        } finally {
            m.unlock();
        }
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

        persistence.finishMatch(m.getMatchId(), winnerId, m.getScores(), m.getCorrectCounts());
        m.setStatus(BattleMatchStatus.FINISHED);

        List<PlayerFinalResultResponse> players = List.of(
                new PlayerFinalResultResponse(m.getUserAId(), sa,
                        m.getCorrectCount(m.getUserAId()), m.getUserAId().equals(winnerId)),
                new PlayerFinalResultResponse(m.getUserBId(), sb,
                        m.getCorrectCount(m.getUserBId()), m.getUserBId().equals(winnerId)));

        MatchFinishedResponse payload =
                new MatchFinishedResponse(m.getMatchId(), winnerId, "COMPLETED", players);
        publisher.sendToUser(m.getUserAId(), "match:finished", payload);
        publisher.sendToUser(m.getUserBId(), "match:finished", payload);
        registry.remove(m);

        log.info("Match finished matchId={} winnerId={} scoreA={} scoreB={}",
                m.getMatchId(), winnerId, sa, sb);
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
            abortMatch(m, "INTERNAL_ERROR");
        }
    }

    private void abortMatch(LiveMatch m, String reason) {
        m.lock();
        try {
            if (m.getStatus() != BattleMatchStatus.IN_PROGRESS) return;
            m.setStatus(BattleMatchStatus.CANCELLED);
            cancelPending(m);
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
}

