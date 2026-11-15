package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.entity.LessonSegment;
import com.elingo.learning.lesson.entity.SegmentWordAccuracy;
import com.elingo.learning.lesson.entity.UserLessonProgress;
import com.elingo.learning.lesson.entity.UserSegmentProgress;
import com.elingo.learning.lesson.enums.LessonMode;

import java.util.List;

public interface LessonProgressService {
    UserSegmentProgress getOrCreateSegmentProgress(Long userId, LessonSegment segment);

    UserLessonProgress recordSegmentSubmitted(Long userId, Long lessonId, LessonMode mode, Integer segmentStartMs);

    void replaceWordsAccuracy(UserSegmentProgress progress, List<SegmentWordAccuracy> wordsAccuracy);
}
