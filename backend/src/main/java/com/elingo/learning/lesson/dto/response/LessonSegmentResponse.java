package com.elingo.learning.lesson.dto.response;

public record LessonSegmentResponse(
        Long id,
        Integer startMs,
        Integer endMs,
        String transcriptOriginal,
        String transcriptNormalized,
        String translation,
        SegmentProgressResponse userProgress
) {
}
