package com.elingo.learning.lesson.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;

import java.math.BigDecimal;
import java.util.List;

public record SegmentProgressResponse(
        Long segmentId,
        Integer dictationAttemptCount,
        BigDecimal dictationBestScore,
        Integer dictationHintUsedCount,
        Integer shadowingAttemptCount,
        BigDecimal shadowingBestScore,
        @BuildMediaUrl String shadowingLatestAudioUrl,
        List<WordAccuracyResponse> wordsAccuracy
) {
}
