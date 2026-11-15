package com.elingo.progress.service.impl;

import com.elingo.learning.flashcard.repository.UserCardStateRepository;
import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.dto.response.WordAccuracyResponse;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.mapper.LessonProgressMapper;
import com.elingo.learning.lesson.repository.SegmentWordAccuracyRepository;
import com.elingo.learning.lesson.repository.UserLessonProgressRepository;
import com.elingo.learning.lesson.repository.UserSegmentProgressRepository;
import com.elingo.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

    private final UserCardStateRepository userCardStateRepository;
    private final UserLessonProgressRepository userLessonProgressRepository;
    private final UserSegmentProgressRepository userSegmentProgressRepository;
    private final SegmentWordAccuracyRepository segmentWordAccuracyRepository;
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

    private Map<Long, Integer> toMap(List<UserCardStateRepository.LearnedCount> rows) {
        return rows.stream().collect(Collectors.toMap(
                UserCardStateRepository.LearnedCount::getRefId,
                r -> Math.toIntExact(r.getLearnedCount()))
        );
    }
}
