package com.elingo.progress.service.impl;

import com.elingo.gamification.entity.XpEvent;
import com.elingo.gamification.entity.XpSource;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.WordAccuracyResponse;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.SegmentWordAccuracyRepository;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import com.elingo.progress.config.ProgressProperties;
import com.elingo.progress.dto.response.DailyStudyProgressResponse;
import com.elingo.progress.dto.response.ProgressOverviewResponse;
import com.elingo.progress.dto.response.SkillProgressResponse;
import com.elingo.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j(topic = "PROGRESS-SERVICE")
@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final ProgressProperties progressProperties;
    private final UserCardStateRepository userCardStateRepository;
    private final UserLessonProgressRepository userLessonProgressRepository;
    private final UserSegmentProgressRepository userSegmentProgressRepository;
    private final SegmentWordAccuracyRepository segmentWordAccuracyRepository;
    private final XpEventRepository xpEventRepository;
    private final LessonProgressMapper lessonProgressMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getLearnedCardCountByDeck(Long userId, Collection<Long> deckIds) {
        if (deckIds.isEmpty()) return Map.of();
        return toMap(userCardStateRepository.countLearnedByDecks(userId, deckIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getLearnedCardCountByTopic(Long userId, Collection<Long> topicIds) {
        if (topicIds.isEmpty()) return Map.of();
        return toMap(userCardStateRepository.countLearnedByTopics(userId, topicIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, LessonProgressSummaryResponse> getLessonProgressSummaryMap(Long userId, Collection<Long> lessonIds) {
        if (lessonIds.isEmpty()) return Map.of();
        return userLessonProgressRepository.findAllByUserIdAndLessonIdIn(userId, lessonIds)
                .stream()
                .collect(Collectors.toMap(
                        progress -> progress.getLesson().getId(),
                        lessonProgressMapper::toLessonProgressSummaryResponse
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonProgressSummaryResponse> findLessonProgressSummary(Long userId, Long lessonId) {
        return userLessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .map(lessonProgressMapper::toLessonProgressSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SegmentProgressResponse buildSegmentProgress(UserSegmentProgress progress) {
        List<WordAccuracyResponse> words = segmentWordAccuracyRepository
                .findAllByUserSegmentProgressIdOrderByIdAsc(progress.getId())
                .stream()
                .map(lessonProgressMapper::toWordAccuracyResponse)
                .toList();
        return lessonProgressMapper.toSegmentProgressResponse(progress, words);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, SegmentProgressResponse> getSegmentProgressMap(Long userId, Long lessonId) {
        List<UserSegmentProgress> progressList = userSegmentProgressRepository.findAllByUserIdAndLessonId(userId, lessonId);
        if (progressList.isEmpty()) return Map.of();

        List<Long> progressIds = progressList.stream().map(UserSegmentProgress::getId).toList();
        Map<Long, List<WordAccuracyResponse>> wordsByProgressId = segmentWordAccuracyRepository
                .findAllByUserSegmentProgressIdInOrderByIdAsc(progressIds)
                .stream()
                .collect(Collectors.groupingBy(
                        word -> word.getUserSegmentProgress().getId(),
                        Collectors.mapping(lessonProgressMapper::toWordAccuracyResponse, Collectors.toList())
                ));

        return progressList.stream().collect(Collectors.toMap(
                progress -> progress.getSegment().getId(),
                progress -> lessonProgressMapper.toSegmentProgressResponse(
                        progress, wordsByProgressId.getOrDefault(progress.getId(), List.of()))
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressOverviewResponse getProgressOverview(Long userId) {
        long totalLearnedCards = userCardStateRepository.countLearnedCardsByUserId(userId);
        UserSegmentProgressRepository.SegmentProgressStats segmentStats =
                userSegmentProgressRepository.getSegmentProgressStats(userId);

        long completedSegments = segmentStats != null ? segmentStats.getCompletedSegments() : 0L;
        long totalStudyMinutes = segmentStats != null ? segmentStats.getEstimatedStudyMs() / 60_000L : 0L;

        log.debug("Progress overview aggregated learnedCards={} completedSegments={} studyMinutes={}",
                totalLearnedCards, completedSegments, totalStudyMinutes);

        return ProgressOverviewResponse.builder()
                .totalLearnedCards(totalLearnedCards)
                .totalCompletedSegments(completedSegments)
                .totalStudyMinutes(totalStudyMinutes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SkillProgressResponse getSkillProgress(Long userId) {
        UserCardStateRepository.FlashcardProgressStats cardStats =
                userCardStateRepository.getFlashcardProgressStats(userId, LocalDateTime.now());
        UserLessonProgressRepository.LessonProgressStats lessonStats =
                userLessonProgressRepository.getLessonProgressStats(userId);
        UserSegmentProgressRepository.SegmentProgressStats segmentStats =
                userSegmentProgressRepository.getSegmentProgressStats(userId);

        long completedDictationLessons = lessonStats != null ? lessonStats.getCompletedDictationLessons() : 0L;
        long inProgressDictationLessons = lessonStats != null ? lessonStats.getInProgressDictationLessons() : 0L;
        long completedShadowingLessons = lessonStats != null ? lessonStats.getCompletedShadowingLessons() : 0L;
        long inProgressShadowingLessons = lessonStats != null ? lessonStats.getInProgressShadowingLessons() : 0L;

        SkillProgressResponse.FlashcardProgress flashcard = SkillProgressResponse.FlashcardProgress.builder()
                .masteredCards(cardStats != null ? cardStats.getMasteredCards() : 0L)
                .dueCards(cardStats != null ? cardStats.getDueCards() : 0L)
                .starredCards(cardStats != null ? cardStats.getStarredCards() : 0L)
                .build();

        SkillProgressResponse.DictationProgress dictation = SkillProgressResponse.DictationProgress.builder()
                .completedLessons(completedDictationLessons)
                .inProgressLessons(inProgressDictationLessons)
                .completedSegments(segmentStats != null ? segmentStats.getCompletedDictationSegments() : 0L)
                .averageScore(roundTwoDecimals(segmentStats != null ? segmentStats.getAverageDictationScore() : null))
                .totalAttempts(segmentStats != null ? segmentStats.getDictationTotalAttempts() : 0L)
                .totalStudyMinutes(segmentStats != null ? segmentStats.getDictationEstimatedStudyMs() / 60_000L : 0L)
                .build();

        SkillProgressResponse.ShadowingProgress shadowing = SkillProgressResponse.ShadowingProgress.builder()
                .completedLessons(completedShadowingLessons)
                .inProgressLessons(inProgressShadowingLessons)
                .completedSegments(segmentStats != null ? segmentStats.getCompletedShadowingSegments() : 0L)
                .averageScore(roundTwoDecimals(segmentStats != null ? segmentStats.getAverageShadowingScore() : null))
                .totalAttempts(segmentStats != null ? segmentStats.getShadowingTotalAttempts() : 0L)
                .totalStudyMinutes(segmentStats != null ? segmentStats.getShadowingEstimatedStudyMs() / 60_000L : 0L)
                .build();

        log.debug("Skill progress aggregated masteredCards={} dictationCompleted={} shadowingCompleted={}",
                flashcard.masteredCards(), dictation.completedLessons(), shadowing.completedLessons());

        return SkillProgressResponse.builder()
                .flashcard(flashcard)
                .dictation(dictation)
                .shadowing(shadowing)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyStudyProgressResponse> getDailyStudyProgress(Long userId, Integer days) {
        int effectiveDays = (days == null || days <= 0)
                ? progressProperties.defaultDailyProgressDays()
                : Math.min(days, progressProperties.maxDailyProgressDays());
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(effectiveDays - 1L);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = today.plusDays(1L).atStartOfDay();

        Map<LocalDate, DailyAccumulator> dailyMap = new LinkedHashMap<>();
        for (int i = 0; i < effectiveDays; i++) {
            LocalDate date = startDate.plusDays(i);
            dailyMap.put(date, new DailyAccumulator());
        }

        List<XpEvent> events = xpEventRepository
                .findAllByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(userId, from, to);

        for (XpEvent event : events) {
            if (event.getCreatedAt() == null || event.getSource() == XpSource.ADMIN_ADJUST) {
                continue;
            }
            DailyAccumulator acc = dailyMap.get(event.getCreatedAt().toLocalDate());
            if (acc == null) {
                continue;
            }
            acc.record(event);
        }

        List<DailyStudyProgressResponse> result = new ArrayList<>(effectiveDays);
        for (Map.Entry<LocalDate, DailyAccumulator> entry : dailyMap.entrySet()) {
            DailyAccumulator acc = entry.getValue();
            result.add(DailyStudyProgressResponse.builder()
                    .date(entry.getKey())
                    .flashcardCount(acc.flashcardCount)
                    .dictationCount(acc.dictationCount)
                    .shadowingCount(acc.shadowingCount)
                    .battleCount(acc.battleCount)
                    .totalActivities(acc.totalActivities)
                    .xpEarned(acc.xpEarned)
                    .build());
        }

        log.debug("Daily study progress aggregated days={} eventCount={}", effectiveDays, events.size());

        return result;
    }

    private double roundTwoDecimals(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class DailyAccumulator {
        private long flashcardCount;
        private long dictationCount;
        private long shadowingCount;
        private long battleCount;
        private long totalActivities;
        private long xpEarned;

        private void record(XpEvent event) {
            XpSource source = event.getSource();
            if (source == XpSource.FLASHCARD_REVIEW) {
                flashcardCount++;
                totalActivities++;
            } else if (source == XpSource.SEGMENT_DICTATION) {
                dictationCount++;
                totalActivities++;
            } else if (source == XpSource.SEGMENT_SHADOWING) {
                shadowingCount++;
                totalActivities++;
            } else if (source == XpSource.BATTLE_WIN || source == XpSource.BATTLE_LOSE) {
                battleCount++;
                totalActivities++;
            } else if (source == XpSource.DAILY_CHECKIN) {
                totalActivities++;
            }
            if (event.getAmount() != null) {
                xpEarned += event.getAmount();
            }
        }
    }

    private Map<Long, Integer> toMap(List<UserCardStateRepository.LearnedCount> rows) {
        return rows.stream().collect(Collectors.toMap(
                UserCardStateRepository.LearnedCount::getRefId,
                r -> Math.toIntExact(r.getLearnedCount()))
        );
    }
}
