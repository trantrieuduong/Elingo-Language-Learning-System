package com.elingo.community.moderation.moderator;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.moderator.impl.OpenAiModerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("unit")
class OpenAiModeratorTest {

    private static final String BASE_URL = "https://api.openai.com";
    private static final String ENDPOINT = "https://api.openai.com/v1/moderations";

    private MockRestServiceServer mockServer;
    private OpenAiModerator moderator;

    @BeforeEach
    void setUp() {
        ModerationProperties properties = new ModerationProperties(
                "omni-moderation-latest",
                BASE_URL,
                "sk-test-openai-key",
                new ModerationProperties.Scheduler(true, 30, 5, 3),
                new ModerationProperties.Policy(0.7, 0.5, 12),
                new ModerationProperties.ModerationService("http://localhost:8000", "token")
        );

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        moderator = new OpenAiModerator(restClient, properties);
    }

    // ========================================================================
    // supports
    // ========================================================================
    @Nested
    @DisplayName("supports")
    class SupportsTests {

        @Test
        @DisplayName("supports() should always return true for any text")
        void supports_AlwaysReturnsTrue() {
            assertThat(moderator.supports("any language or text")).isTrue();
            assertThat(moderator.supports("")).isTrue();
        }
    }

    // ========================================================================
    // moderateText
    // ========================================================================
    @Nested
    @DisplayName("moderateText")
    class ModerateTextTests {

        @Test
        @DisplayName("Should return clean when all category scores are below review threshold")
        void moderateText_WhenBelowReviewThreshold_ReturnsClean() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {
                                "id": "modr-123",
                                "model": "omni-moderation-latest",
                                "results": [{
                                    "flagged": false,
                                    "categories": {"hate": false, "violence": false},
                                    "category_scores": {"hate": 0.01, "violence": 0.05, "sexual": 0.1}
                                }]
                            }
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Just a peaceful text");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
            assertThat(result.label()).isEqualTo("clean");
            assertThat(result.score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return AUTO_REJECT with top category when score exceeds auto reject threshold")
        void moderateText_WhenAboveAutoRejectThreshold_ReturnsAutoReject() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {
                                "id": "modr-456",
                                "model": "omni-moderation-latest",
                                "results": [{
                                    "flagged": true,
                                    "categories": {"violence": true, "hate": false},
                                    "category_scores": {"violence": 0.92, "hate": 0.35, "harassment": 0.40}
                                }]
                            }
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Extremely violent description");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_REJECT);
            assertThat(result.label()).isEqualTo("violence");
            assertThat(result.score()).isEqualTo(0.92);
        }

        @Test
        @DisplayName("Should return FLAG_FOR_REVIEW when score is between review and reject thresholds")
        void moderateText_WhenBetweenThresholds_ReturnsFlagForReview() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {
                                "id": "modr-789",
                                "model": "omni-moderation-latest",
                                "results": [{
                                    "flagged": false,
                                    "categories": {"harassment": false},
                                    "category_scores": {"harassment": 0.65, "hate": 0.20}
                                }]
                            }
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Borderline harassment text");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.FLAG_FOR_REVIEW);
            assertThat(result.label()).isEqualTo("harassment");
            assertThat(result.score()).isEqualTo(0.65);
        }
    }

    // ========================================================================
    // moderateImage
    // ========================================================================
    @Nested
    @DisplayName("moderateImage")
    class ModerateImageTests {

        @Test
        @DisplayName("Should moderate image from public URL and return evaluated result")
        void moderateImage_WithUrl_ReturnsEvaluatedResult() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {
                                "id": "modr-img-1",
                                "model": "omni-moderation-latest",
                                "results": [{
                                    "flagged": false,
                                    "categories": {},
                                    "category_scores": {"sexual": 0.02}
                                }]
                            }
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateImage("https://r2.elingo.pro/sample.jpg");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
        }

        @Test
        @DisplayName("Should moderate image from bytes and return evaluated result")
        void moderateImage_WithBytes_ReturnsEvaluatedResult() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {
                                "id": "modr-img-2",
                                "model": "omni-moderation-latest",
                                "results": [{
                                    "flagged": false,
                                    "categories": {},
                                    "category_scores": {"hate": 0.001}
                                }]
                            }
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateImage("image/jpeg", new byte[]{1, 2, 3});

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
        }
    }

    // ========================================================================
    // errorHandling
    // ========================================================================
    @Nested
    @DisplayName("errorHandling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw ModerationUnavailableException when response has empty results")
        void call_WhenEmptyResults_ThrowsModerationUnavailableException() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andRespond(withSuccess("""
                            {"id": "modr-empty", "results": []}
                            """, MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> moderator.moderateText("Hello"))
                    .isInstanceOf(ModerationUnavailableException.class)
                    .hasMessageContaining("OpenAI return empty response");

            mockServer.verify();
        }

        @Test
        @DisplayName("Should throw ModerationUnavailableException when OpenAI returns HTTP 503")
        void call_WhenOpenAiReturns503_ThrowsModerationUnavailableException() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

            assertThatThrownBy(() -> moderator.moderateText("Hello"))
                    .isInstanceOf(ModerationUnavailableException.class)
                    .hasMessageContaining("OpenAI returned HTTP 503");

            mockServer.verify();
        }

        @Test
        @DisplayName("Should throw UnprocessableContentException when OpenAI returns HTTP 400")
        void call_WhenOpenAiReturns400_ThrowsUnprocessableContentException() {
            mockServer.expect(requestTo(ENDPOINT))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> moderator.moderateText("Hello"))
                    .isInstanceOf(UnprocessableContentException.class)
                    .hasMessageContaining("OpenAI returned HTTP 400");

            mockServer.verify();
        }
    }
}
