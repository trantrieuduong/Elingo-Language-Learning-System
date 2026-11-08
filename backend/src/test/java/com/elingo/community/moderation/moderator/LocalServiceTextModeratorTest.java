package com.elingo.community.moderation.moderator;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.moderator.impl.LocalServiceTextModerator;
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
class LocalServiceTextModeratorTest {

    private static final String BASE_URL = "http://localhost:8000/v1/moderate/text";

    private MockRestServiceServer mockServer;
    private LocalServiceTextModerator moderator;

    @BeforeEach
    void setUp() {
        ModerationProperties properties = new ModerationProperties(
                "omni-moderation-latest",
                "https://api.openai.com",
                "test-api-key",
                new ModerationProperties.Scheduler(true, 30, 5, 3),
                new ModerationProperties.Policy(0.7, 0.5, 12),
                new ModerationProperties.ModerationService(BASE_URL, "secret-token")
        );

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        moderator = new LocalServiceTextModerator(restClient, properties);
    }

    // ========================================================================
    // supports
    // ========================================================================
    @Nested
    @DisplayName("supports")
    class SupportsTests {

        @Test
        @DisplayName("Should return true for Vietnamese text and false for pure English")
        void supports_DistinguishesVietnamese() {
            assertThat(moderator.supports("Bài viết rất hay, cảm ơn bạn")).isTrue();
            assertThat(moderator.supports("This is pure English content")).isFalse();
        }
    }

    // ========================================================================
    // moderateText
    // ========================================================================
    @Nested
    @DisplayName("moderateText")
    class ModerateTextTests {

        @Test
        @DisplayName("Should return clean when label is CLEAN")
        void moderateText_WhenCleanLabel_ReturnsClean() {
            mockServer.expect(requestTo(BASE_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"label": "CLEAN", "score": 0.98, "scores": {"CLEAN": 0.98, "OFFENSIVE": 0.02}, "chunks": 1}
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Hôm nay thời tiết thật đẹp");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
            assertThat(result.label()).isEqualTo("clean");
            assertThat(result.score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return clean when score is below review threshold")
        void moderateText_WhenScoreBelowReviewThreshold_ReturnsClean() {
            // reviewThreshold is 0.5, score is 0.4
            mockServer.expect(requestTo(BASE_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"label": "OFFENSIVE", "score": 0.4, "scores": {"CLEAN": 0.6, "OFFENSIVE": 0.4}, "chunks": 1}
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Một chút hơi gắt gỏng");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_APPROVE);
            assertThat(result.label()).isEqualTo("clean");
        }

        @Test
        @DisplayName("Should return AUTO_REJECT when score exceeds auto reject threshold")
        void moderateText_WhenScoreAboveAutoRejectThreshold_ReturnsAutoReject() {
            // autoRejectThreshold is 0.7, score is 0.85
            mockServer.expect(requestTo(BASE_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"label": "HATE", "score": 0.85, "scores": {"CLEAN": 0.05, "HATE": 0.85}, "chunks": 1}
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Nội dung mang tính thù địch nặng nề");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.AUTO_REJECT);
            assertThat(result.label()).isEqualTo("hate");
            assertThat(result.score()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("Should return FLAG_FOR_REVIEW when score is between review and reject thresholds")
        void moderateText_WhenScoreBetweenThresholds_ReturnsFlagForReview() {
            // reviewThreshold = 0.5, autoRejectThreshold = 0.7, score = 0.62
            mockServer.expect(requestTo(BASE_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("""
                            {"label": "OFFENSIVE", "score": 0.62, "scores": {"CLEAN": 0.38, "OFFENSIVE": 0.62}, "chunks": 1}
                            """, MediaType.APPLICATION_JSON));

            ModerationResult result = moderator.moderateText("Nội dung nghi vấn xúc phạm");

            mockServer.verify();
            assertThat(result.action()).isEqualTo(ModerationAction.FLAG_FOR_REVIEW);
            assertThat(result.label()).isEqualTo("offensive");
            assertThat(result.score()).isEqualTo(0.62);
        }

        @Test
        @DisplayName("Should throw ModerationUnavailableException when response body is empty or invalid")
        void moderateText_WhenEmptyResponse_ThrowsModerationUnavailableException() {
            mockServer.expect(requestTo(BASE_URL))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> moderator.moderateText("Một bài viết hợp lệ"))
                    .isInstanceOf(ModerationUnavailableException.class)
                    .hasMessageContaining("return empty response");

            mockServer.verify();
        }

        @Test
        @DisplayName("Should throw ModerationUnavailableException on HTTP 500")
        void moderateText_WhenServerReturns500_ThrowsModerationUnavailableException() {
            mockServer.expect(requestTo(BASE_URL))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> moderator.moderateText("Thử kiểm tra lỗi server"))
                    .isInstanceOf(ModerationUnavailableException.class)
                    .hasMessageContaining("moderation-service returned HTTP 500");

            mockServer.verify();
        }

        @Test
        @DisplayName("Should throw UnprocessableContentException on HTTP 422")
        void moderateText_WhenServerReturns422_ThrowsUnprocessableContentException() {
            mockServer.expect(requestTo(BASE_URL))
                    .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

            assertThatThrownBy(() -> moderator.moderateText("Văn bản không hợp lệ"))
                    .isInstanceOf(UnprocessableContentException.class)
                    .hasMessageContaining("moderation-service returned HTTP 422");

            mockServer.verify();
        }
    }
}
