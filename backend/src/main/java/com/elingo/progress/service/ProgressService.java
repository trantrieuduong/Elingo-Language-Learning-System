package com.elingo.progress.service;

import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.UserSegmentProgress;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ProgressService {
    Map<Long, Integer> getLearnedCardCountByDeck(Long userId, Collection<Long> deckIds);
    Map<Long, Integer> getLearnedCardCountByTopic(Long userId, Collection<Long> topicIds);

    Map<Long, LessonProgressSummaryResponse> getLessonProgressSummaryMap(Long userId, Collection<Long> lessonIds);
    Optional<LessonProgressSummaryResponse> findLessonProgressSummary(Long userId, Long lessonId);

    Map<Long, SegmentProgressResponse> getSegmentProgressMap(Long userId, Long lessonId);
    SegmentProgressResponse buildSegmentProgress(UserSegmentProgress progress);
}
