package com.elingo.community.moderation.scheduler;

import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.repository.PostModerationRepository;
import com.elingo.community.moderation.service.PostModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostModerationSchedulerTest {

    @Mock
    private PostModerationRepository repository;

    @Mock
    private PostModerationService service;

    private PostModerationScheduler scheduler;
    private ModerationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ModerationProperties(
                "omni-moderation-latest",
                "https://api.openai.com",
                "test-api-key",
                new ModerationProperties.Scheduler(true, 30, 5, 3),
                new ModerationProperties.Policy(0.7, 0.5, 12),
                new ModerationProperties.ModerationService("http://localhost:8000", "token")
        );

        scheduler = new PostModerationScheduler(repository, service, properties);
    }

    // ========================================================================
    // moderatePendingPosts
    // ========================================================================
    @Nested
    @DisplayName("moderatePendingPosts")
    class ModeratePendingPostsTests {

        @Test
        @DisplayName("Should do nothing when there are no pending posts")
        void moderatePendingPosts_WhenBatchEmpty_DoesNothing() {
            when(repository.findPending(5)).thenReturn(List.of());

            scheduler.moderatePendingPosts();

            verify(repository).findPending(5);
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Should successfully moderate all posts in batch")
        void moderatePendingPosts_WhenSuccessful_ModeratesAllPosts() {
            PostForModeration post1 = new PostForModeration(1L, 10L, "Title 1", "Content 1", List.of());
            PostForModeration post2 = new PostForModeration(2L, 20L, "Title 2", "Content 2", List.of());

            when(repository.findPending(5)).thenReturn(List.of(post1, post2));

            scheduler.moderatePendingPosts();

            verify(service).moderate(post1);
            verify(service).moderate(post2);
        }

        @Test
        @DisplayName("Should pause and stop processing remaining batch on ModerationUnavailableException")
        void moderatePendingPosts_WhenProviderUnavailable_PausesAndAbortsRemaining() {
            PostForModeration post1 = new PostForModeration(1L, 10L, "Title 1", "Content 1", List.of());
            PostForModeration post2 = new PostForModeration(2L, 20L, "Title 2", "Content 2", List.of());

            when(repository.findPending(5)).thenReturn(List.of(post1, post2));
            doThrow(new ModerationUnavailableException("Local moderation service down"))
                    .when(service).moderate(post1);

            scheduler.moderatePendingPosts();

            verify(service).moderate(post1);
            verify(service, never()).moderate(post2);

            // Next call immediately should be paused
            reset(repository);
            scheduler.moderatePendingPosts();
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should flag for review and continue to next post on UnprocessableContentException")
        void moderatePendingPosts_WhenUnprocessableContent_FlagsForReviewAndContinues() {
            PostForModeration post1 = new PostForModeration(1L, 10L, "Title 1", "Content 1", List.of());
            PostForModeration post2 = new PostForModeration(2L, 20L, "Title 2", "Content 2", List.of());

            when(repository.findPending(5)).thenReturn(List.of(post1, post2));
            doThrow(new UnprocessableContentException("Video corrupted"))
                    .when(service).moderate(post1);

            scheduler.moderatePendingPosts();

            verify(service).moderate(post1);
            verify(service).flagForReview(post1, "Video corrupted");
            verify(service).moderate(post2);
        }

        @Test
        @DisplayName("Should retry on unexpected exception until max attempts, then flag for review")
        void moderatePendingPosts_WhenUnexpectedException_RetriesUntilMaxAttempts() {
            PostForModeration post = new PostForModeration(1L, 10L, "Title", "Content", List.of());

            when(repository.findPending(anyInt())).thenReturn(List.of(post));
            doThrow(new NullPointerException("Unexpected NPE")).when(service).moderate(post);

            // Attempt 1: only fails and counts failure
            scheduler.moderatePendingPosts();
            verify(service, never()).flagForReview(any(), any());

            // Attempt 2: still under maxAttempts (3)
            scheduler.moderatePendingPosts();
            verify(service, never()).flagForReview(any(), any());

            // Attempt 3: reaches maxAttempts (3) -> flags for review
            scheduler.moderatePendingPosts();
            verify(service, times(1)).flagForReview(eq(post), eq("Processing error: NullPointerException"));
        }
    }
}
