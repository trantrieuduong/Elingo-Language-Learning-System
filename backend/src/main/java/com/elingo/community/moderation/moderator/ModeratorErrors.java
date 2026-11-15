package com.elingo.community.moderation.moderator;

import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public final class ModeratorErrors {

    private ModeratorErrors() {}

    public static RuntimeException http(String provider, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String message = provider + " returned HTTP " + status;
        boolean transientError = status >= 500 || status == 429 || status == 408 || status == 401 || status == 403;
        return transientError
                ? new ModerationUnavailableException(message, e)
                : new UnprocessableContentException(message, e);
    }

    public static ModerationUnavailableException io(String provider, RestClientException e) {
        return new ModerationUnavailableException(provider + " did not response: " + e.getMessage(), e);
    }
}
