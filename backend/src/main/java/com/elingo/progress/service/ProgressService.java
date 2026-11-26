package com.elingo.progress.service;

import com.elingo.learning.lesson.dto.response.LessonProgressSummaryResponse;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.progress.dto.response.DailyStudyProgressResponse;
import com.elingo.progress.dto.response.ProgressOverviewResponse;
import com.elingo.progress.dto.response.SkillProgressResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProgressService {
    Map<Long, Integer> getLearnedCardCountByDeck(Long userId, Collection<Long> deckIds);
    Map<Long, Integer> getLearnedCardCountByTopic(Long userId, Collection<Long> topicIds);

    Map<Long, LessonProgressSummaryResponse> getLessonProgressSummaryMap(Long userId, Collection<Long> lessonIds);
    Optional<LessonProgressSummaryResponse> findLessonProgressSummary(Long userId, Long lessonId);

    Map<Long, SegmentProgressResponse> getSegmentProgressMap(Long userId, Long lessonId);
    SegmentProgressResponse buildSegmentProgress(UserSegmentProgress progress);

    ProgressOverviewResponse getProgressOverview(Long userId);
    SkillProgressResponse getSkillProgress(Long userId);
    List<DailyStudyProgressResponse> getDailyStudyProgress(Long userId, Integer days);
}
