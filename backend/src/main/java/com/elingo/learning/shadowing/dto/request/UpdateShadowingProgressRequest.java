package com.elingo.learning.shadowing.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateShadowingProgressRequest(
        @NotNull(message = "ATTEMPT_COUNT_INVALID")
        @Min(value = 1, message = "ATTEMPT_COUNT_INVALID")
        @Max(value = 1000, message = "ATTEMPT_COUNT_INVALID")
        Integer attemptCount,

        @NotNull(message = "SHADOWING_SCORE_INVALID")
        @Min(value = 0, message = "SHADOWING_SCORE_INVALID")
        @Max(value = 100, message = "SHADOWING_SCORE_INVALID")
        Integer score,

        @NotNull(message = "WORDS_ACCURACY_INVALID")
        @Size(max = 300, message = "WORDS_ACCURACY_INVALID")
        @Valid
        List<@NotNull(message = "WORDS_ACCURACY_INVALID") WordAccuracyItem> wordsAccuracy,

        @NotBlank(message = "AUDIO_RECORDING_REQUIRED")
        @BuildMediaKey
        @AllowedMediaKind(MediaKind.AUDIO)
        @Size(max = 255, message = "INVALID_REQUEST")
        String audioUrl
) {
}
