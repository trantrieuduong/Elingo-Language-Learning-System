package com.elingo.community.moderation.moderator.impl;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.moderator.ModeratorErrors;
import com.elingo.community.moderation.moderator.TextModerator;
import com.elingo.community.moderation.service.VietnameseDetector;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;
import java.util.Map;

/** Tiếng Việt -> moderation-service nội bộ */
@Slf4j(topic = "LOCAL-SERVICE-TEXT-MODERATOR")
@Component
@Order(10)
public class LocalServiceTextModerator implements TextModerator {

    private final RestClient client;
    private final ModerationProperties properties;

    public LocalServiceTextModerator(@Qualifier("localModerationServiceClient") RestClient client,
                                     ModerationProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public boolean supports(String text) {
        return VietnameseDetector.containVietnamese(text);
    }

    @Override
    public ModerationResult moderateText(String text) {
        Response res;
        try {
            res = client.post()
                    .body(new Request(text))
                    .retrieve()
                    .body(Response.class);
        } catch (RestClientResponseException e) {
            throw ModeratorErrors.http("moderation-service", e);
        } catch (RestClientException e) {
            throw ModeratorErrors.io("moderation-service", e);
        }

        return decide(res);
    }

    private ModerationResult decide(Response response) {
        if (response == null || response.scores() == null || response.label() == null)
            throw new ModerationUnavailableException("moderation-service return empty response.");

        log.debug("Local text moderation evaluated label={} score={}", response.label(), response.score());

        // Bình luận không chứa ngôn từ thù ghét hoặc xúc phạm
        String CLEAN = "CLEAN";
        if (response.label().equals(CLEAN) || response.score() < properties.policy().reviewThreshold())
            return ModerationResult.clean();

        return new ModerationResult(
                response.score() > properties.policy().autoRejectThreshold()
                        ? ModerationAction.AUTO_REJECT
                        : ModerationAction.FLAG_FOR_REVIEW,
                response.label().toLowerCase(Locale.ROOT),
                response.score(),
                null
        );
    }

    record Request(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(String label, double score, Map<String, Double> scores, int chunks) {}
}
