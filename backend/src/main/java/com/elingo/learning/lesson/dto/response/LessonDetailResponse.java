package com.elingo.learning.lesson.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.vocabulary.dto.response.CefrLevelResponse;
import com.elingo.vocabulary.dto.response.TagResponse;

import java.time.LocalDateTime;
import java.util.Set;

public record LessonDetailResponse(
        Long id,
        String title,
        String slug,
        String description,
        Boolean isPremium,
        Integer durationMs,
        String sourceUrl,           // URL YouTube đầy đủ — KHÔNG gắn @BuildMediaUrl
        @BuildMediaUrl String thumbnailUrl,
        LocalDateTime publishedAt,
        Set<TagResponse> tags,
        Set<CefrLevelResponse> cefrLevels,
        LessonProgressSummaryResponse userProgress,
        Integer segmentCount,
        Integer dictationPassScore,
        Integer shadowingPassScore
) {
}
