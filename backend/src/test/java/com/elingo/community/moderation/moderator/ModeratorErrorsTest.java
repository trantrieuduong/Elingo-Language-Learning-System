package com.elingo.community.moderation.moderator;

import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class ModeratorErrorsTest {

    // ========================================================================
    // http
    // ========================================================================
    @Nested
    @DisplayName("http")
    class HttpTests {

        @ParameterizedTest
        @ValueSource(ints = {500, 502, 503, 504, 429, 408, 401, 403})
        @DisplayName("Should return ModerationUnavailableException for transient or auth error statuses")
        void http_WhenTransientOrAuthStatus_ReturnsModerationUnavailableException(int statusCode) {
            RestClientResponseException mockEx = mock(RestClientResponseException.class);
            when(mockEx.getStatusCode()).thenReturn(HttpStatusCode.valueOf(statusCode));

            RuntimeException result = ModeratorErrors.http("OpenAI", mockEx);

            assertThat(result)
                    .isInstanceOf(ModerationUnavailableException.class)
                    .hasMessage("OpenAI returned HTTP " + statusCode)
                    .hasCause(mockEx);
        }

        @ParameterizedTest
        @ValueSource(ints = {400, 422, 404, 413, 415})
        @DisplayName("Should return UnprocessableContentException for non-transient client error statuses")
        void http_WhenNonTransientClientStatus_ReturnsUnprocessableContentException(int statusCode) {
            RestClientResponseException mockEx = mock(RestClientResponseException.class);
            when(mockEx.getStatusCode()).thenReturn(HttpStatusCode.valueOf(statusCode));

            RuntimeException result = ModeratorErrors.http("moderation-service", mockEx);

            assertThat(result)
                    .isInstanceOf(UnprocessableContentException.class)
                    .hasMessage("moderation-service returned HTTP " + statusCode)
                    .hasCause(mockEx);
        }
    }

    // ========================================================================
    // io
    // ========================================================================
    @Nested
    @DisplayName("io")
    class IoTests {

        @Test
        @DisplayName("Should return ModerationUnavailableException with provider message")
        void io_ReturnsModerationUnavailableException() {
            ResourceAccessException ioEx = new ResourceAccessException("Connection timed out");

            ModerationUnavailableException result = ModeratorErrors.io("moderation-service", ioEx);

            assertThat(result)
                    .isNotNull()
                    .hasMessage("moderation-service did not response: Connection timed out")
                    .hasCause(ioEx);
        }
    }
}
