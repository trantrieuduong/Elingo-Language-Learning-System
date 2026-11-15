package com.elingo.community.moderation.service;

import com.elingo.common.event.PostModeratedEvent;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.repository.PostModerationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostModerationServiceTest {

    @Mock
    private PostModerationRepository repository;

    @Mock
    private ModerationPipeline pipeline;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private PostModerationService service;

    // ========================================================================
    // moderate
    // ========================================================================
    @Nested
    @DisplayName("moderate")
    class ModerateTests {

        @Test
        @DisplayName("Should run pipeline, apply decision, and publish event when decision applied successfully")
        void moderate_WhenDecisionApplied_PublishesEvent() {
            PostForModeration post = new PostForModeration(100L, 200L, "Title", "Content", List.of());
            ModerationResult result = new ModerationResult(ModerationAction.AUTO_REJECT, "hate", 0.95, null);

            when(pipeline.run(post)).thenReturn(result);
            when(repository.applyDecision(100L, result)).thenReturn(true);

            service.moderate(post);

            verify(pipeline).run(post);
            verify(repository).applyDecision(100L, result);

            ArgumentCaptor<PostModeratedEvent> eventCaptor = ArgumentCaptor.forClass(PostModeratedEvent.class);
            verify(events).publishEvent(eventCaptor.capture());

            PostModeratedEvent published = eventCaptor.getValue();
            assertThat(published.postId()).isEqualTo(100L);
            assertThat(published.userId()).isEqualTo(200L);
            assertThat(published.action()).isEqualTo(ModerationAction.AUTO_REJECT);
        }

        @Test
        @DisplayName("Should not publish event when post is no longer PENDING (applyDecision returns false)")
        void moderate_WhenPostNoLongerPending_DoesNotPublishEvent() {
            PostForModeration post = new PostForModeration(100L, 200L, "Title", "Content", List.of());
            ModerationResult result = ModerationResult.clean();

            when(pipeline.run(post)).thenReturn(result);
            when(repository.applyDecision(100L, result)).thenReturn(false);

            service.moderate(post);

            verify(pipeline).run(post);
            verify(repository).applyDecision(100L, result);
            verifyNoInteractions(events);
        }
    }

    // ========================================================================
    // flagForReview
    // ========================================================================
    @Nested
    @DisplayName("flagForReview")
    class FlagForReviewTests {

        @Test
        @DisplayName("Should apply FLAG_FOR_REVIEW decision with reason and publish event")
        void flagForReview_WhenApplied_PublishesEvent() {
            PostForModeration post = new PostForModeration(100L, 200L, "Title", "Content", List.of());
            String reason = "Video frame extraction timeout";

            ArgumentCaptor<ModerationResult> resultCaptor = ArgumentCaptor.forClass(ModerationResult.class);
            when(repository.applyDecision(eq(100L), resultCaptor.capture())).thenReturn(true);

            service.flagForReview(post, reason);

            ModerationResult appliedResult = resultCaptor.getValue();
            assertThat(appliedResult.action()).isEqualTo(ModerationAction.FLAG_FOR_REVIEW);
            assertThat(appliedResult.label()).isEqualTo("processing_error");
            assertThat(appliedResult.score()).isEqualTo(0.0);
            assertThat(appliedResult.note()).isEqualTo(reason);

            verify(events).publishEvent(any(PostModeratedEvent.class));
        }
    }
}
