package com.elingo.learning.dictation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateDictationProgressRequest(
        @NotNull(message = "ATTEMPT_COUNT_INVALID")
        @Min(value = 1, message = "ATTEMPT_COUNT_INVALID")
        @Max(value = 1000, message = "ATTEMPT_COUNT_INVALID")
        Integer attemptCount,

        @NotNull(message = "HINT_COUNT_INVALID")
        @Min(value = 0, message = "HINT_COUNT_INVALID")
        @Max(value = 10000, message = "HINT_COUNT_INVALID")
        Integer hintUsedCount
) {
}
