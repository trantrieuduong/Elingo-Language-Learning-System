package com.elingo.community.moderation.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

// https://developers.openai.com/api/docs/guides/moderation?api-mode=responses
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiModerationResponse(String id,
                                       String model,
                                       List<Result> results) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            boolean flagged,
            Map<String, Boolean> categories,
            @JsonProperty("category_scores") Map<String, Double> categoryScores,
            @JsonProperty("category_applied_input_types") Map<String, List<String>> categoryAppliedInputTypes
    ) {}
}
