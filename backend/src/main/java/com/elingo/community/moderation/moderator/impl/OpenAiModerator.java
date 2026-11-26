package com.elingo.community.moderation.moderator.impl;

import com.elingo.common.enums.SystemConfigKey;
import com.elingo.common.service.SystemConfigService;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.dto.request.OpenAiModerationRequest;
import com.elingo.community.moderation.dto.response.OpenAiModerationResponse;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.moderator.ImageModerator;
import com.elingo.community.moderation.moderator.ModeratorErrors;
import com.elingo.community.moderation.moderator.TextModerator;
import com.elingo.community.moderation.model.ModerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j(topic = "OPENAI-MODERATOR")
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OpenAiModerator implements TextModerator, ImageModerator {

    private static final String DEFAULT_MODEL = "omni-moderation-latest";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";

    private final RestClient openAiRestClient;
    private final ModerationProperties properties;
    private final SystemConfigService systemConfigService;

    private static final String[] CATEGORIES = {
            "sexual",
            "sexual/minors",
            "harassment",
            "harassment/threatening",
            "hate",
            "hate/threatening",
            "self-harm",
            "self-harm/intent",
            "self-harm/instructions",
            "violence",
            "violence/graphic"
    };

    public OpenAiModerator(@Qualifier("openAiRestClient") RestClient openAiRestClient,
                           ModerationProperties properties,
                           SystemConfigService systemConfigService) {
        this.openAiRestClient = openAiRestClient;
        this.properties = properties;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public ModerationResult moderateImage(String publicImageUrl) {
        return call(new OpenAiModerationRequest.ImageInput(
                OpenAiModerationRequest.ImageUrl.from(publicImageUrl))
        );
    }

    @Override
    public ModerationResult moderateImage(String mimeType, byte[] bytes) {
        return call(new OpenAiModerationRequest.ImageInput(
                OpenAiModerationRequest.ImageUrl.from(mimeType, bytes))
        );
    }

    @Override
    public boolean supports(String text) {
        return true;
    }

    @Override
    public ModerationResult moderateText(String text) {
        return call(new OpenAiModerationRequest.TextInput(text));
    }

    private ModerationResult call(OpenAiModerationRequest.Input input) {
        checkApiKeyConfigured();

        OpenAiModerationResponse res;
        try {
            res = openAiRestClient.post()
                    .uri(resolveUri())
                    .body(new OpenAiModerationRequest(resolveModel(), List.of(input)))
                    .retrieve()
                    .body(OpenAiModerationResponse.class);
        } catch (RestClientResponseException e) {
            throw ModeratorErrors.http("OpenAI", e);
        } catch (RestClientException e) {
            throw ModeratorErrors.io("OpenAI", e);
        }
        if (res == null || res.results() == null || res.results().isEmpty()) {
            throw new ModerationUnavailableException("OpenAI return empty response.");
        }
        return decide(res.results().getFirst().categoryScores());
    }

    private void checkApiKeyConfigured() {
        String apiKey = systemConfigService.getString(SystemConfigKey.MODERATION_OPENAI_API_KEY);
        if (apiKey == null || apiKey.isBlank()) {
            throw new ModerationUnavailableException("OpenAI moderation API key is not configured in SystemConfig");
        }
    }

    private String resolveModel() {
        String model = systemConfigService.getString(SystemConfigKey.MODERATION_OPENAI_MODEL);
        if (model != null && !model.isBlank()) {
            return model;
        }
        return DEFAULT_MODEL;
    }

    private String resolveUri() {
        String baseUrl = systemConfigService.getString(SystemConfigKey.MODERATION_OPENAI_BASE_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        int end = baseUrl.length();
        while (end > 0 && baseUrl.charAt(end - 1) == '/') {
            end--;
        }
        return baseUrl.substring(0, end) + "/v1/moderations";
    }

    private ModerationResult decide(Map<String, Double> scores) {
        if (scores == null) {
            throw new ModerationUnavailableException("OpenAI returned no category scores");
        }
        String top = null;
        double max = 0.0;
        for (String c : CATEGORIES) {
            double s = scores.getOrDefault(c, 0.0);
            if (s > max) {
                max = s;
                top = c;
            }
        }

        log.debug("OpenAI moderation evaluated topCategory={} maxScore={}", top, max);

        if (top == null || max <= properties.policy().reviewThreshold()) {
            return ModerationResult.clean();
        }
        ModerationAction action = max > properties.policy().autoRejectThreshold()
                ? ModerationAction.AUTO_REJECT : ModerationAction.FLAG_FOR_REVIEW;
        return new ModerationResult(action, top, max, null);
    }
}
