package com.elingo.learning.lesson.dto.response;

import com.elingo.learning.lesson.entity.ProgressStatus;

import java.math.BigDecimal;

public record LessonProgressSummaryResponse(
        ProgressStatus dictationStatus,
        BigDecimal dictationProgressPct,
        Integer dictationLastStartMs,
        ProgressStatus shadowingStatus,
        BigDecimal shadowingProgressPct,
        Integer shadowingLastStartMs
) {
}
