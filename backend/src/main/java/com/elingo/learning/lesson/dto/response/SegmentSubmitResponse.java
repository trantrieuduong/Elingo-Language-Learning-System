package com.elingo.learning.lesson.dto.response;

public record SegmentSubmitResponse(
        Integer score,
        Boolean passed,
        SegmentProgressResponse segmentProgress,
        LessonProgressSummaryResponse lessonProgress
) {
}
