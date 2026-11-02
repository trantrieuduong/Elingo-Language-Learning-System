package com.elingo.learning.lesson.service;

import com.elingo.common.enums.LessonMode;
import com.elingo.learning.lesson.dto.response.SegmentProgressResponse;
import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;

import java.util.List;
import java.util.Map;

public interface LessonProgressService {
    UserSegmentProgress getOrCreateSegmentProgress(Long userId, LessonSegment segment);

    UserLessonProgress recordSegmentSubmitted(Long userId, Long lessonId, LessonMode mode, Integer segmentStartMs);

    SegmentProgressResponse buildSegmentProgress(UserSegmentProgress progress);

    Map<Long, SegmentProgressResponse> getSegmentProgressMap(Long userId, Long lessonId);

    void replaceWordsAccuracy(UserSegmentProgress progress, List<SegmentWordAccuracy> wordsAccuracy);
}
