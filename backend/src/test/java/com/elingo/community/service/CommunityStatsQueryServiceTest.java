package com.elingo.community.service;

import com.elingo.community.entity.PostStatus;
import com.elingo.community.moderation.repository.PostModerationRepository;
import com.elingo.community.repository.CommentRepository;
import com.elingo.community.repository.PostLikeRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.impl.CommunityStatsQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CommunityStatsQueryServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostModerationRepository postModerationRepository;

    @InjectMocks
    private CommunityStatsQueryServiceImpl communityStatsQueryService;

    private final LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 11, 4, 0, 0);

    @Nested
    @DisplayName("countPostsByStatus")
    class CountPostsByStatusTests {

        @Test
        @DisplayName("Should delegate to postRepository.countPostsByStatus")
        void countPostsByStatus_Success() {
            PostRepository.PostStatusCountProjection proj = mock(PostRepository.PostStatusCountProjection.class);
            when(proj.getStatus()).thenReturn(PostStatus.PUBLISHED);
            when(proj.getCountVal()).thenReturn(100L);
            when(postRepository.countPostsByStatus()).thenReturn(List.of(proj));

            List<PostRepository.PostStatusCountProjection> result = communityStatsQueryService.countPostsByStatus();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(result.get(0).getCountVal()).isEqualTo(100L);
            verify(postRepository).countPostsByStatus();
        }
    }

    @Nested
    @DisplayName("countPostsByGroup")
    class CountPostsByGroupTests {

        @Test
        @DisplayName("Should delegate to postRepository.countPostsByGroup")
        void countPostsByGroup_Success() {
            PostRepository.PostGroupCountProjection proj = mock(PostRepository.PostGroupCountProjection.class);
            when(proj.getGroupId()).thenReturn(1L);
            when(proj.getPostCount()).thenReturn(50L);
            when(postRepository.countPostsByGroup()).thenReturn(List.of(proj));

            List<PostRepository.PostGroupCountProjection> result = communityStatsQueryService.countPostsByGroup();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGroupId()).isEqualTo(1L);
            assertThat(result.get(0).getPostCount()).isEqualTo(50L);
            verify(postRepository).countPostsByGroup();
        }
    }

    @Nested
    @DisplayName("countPostBacklog")
    class CountPostBacklogTests {

        @Test
        @DisplayName("Should delegate to postRepository.countPostBacklog")
        void countPostBacklog_Success() {
            PostRepository.PostBacklogProjection proj = mock(PostRepository.PostBacklogProjection.class);
            when(proj.getPendingCount()).thenReturn(3L);
            when(proj.getNeedsReviewCount()).thenReturn(7L);
            when(postRepository.countPostBacklog()).thenReturn(proj);

            PostRepository.PostBacklogProjection result = communityStatsQueryService.countPostBacklog();

            assertThat(result).isNotNull();
            assertThat(result.getPendingCount()).isEqualTo(3L);
            assertThat(result.getNeedsReviewCount()).isEqualTo(7L);
            verify(postRepository).countPostBacklog();
        }
    }

    @Nested
    @DisplayName("countLikesByDay and countCommentsByDay")
    class EngagementTimeSeriesTests {

        @Test
        @DisplayName("Should delegate to postLikeRepository.countLikesByDay")
        void countLikesByDay_Success() {
            PostLikeRepository.DateCountProjection proj = mock(PostLikeRepository.DateCountProjection.class);
            when(proj.getDateKey()).thenReturn("2026-11-01");
            when(proj.getCountVal()).thenReturn(30L);
            when(postLikeRepository.countLikesByDay(from, to)).thenReturn(List.of(proj));

            List<PostLikeRepository.DateCountProjection> result = communityStatsQueryService.countLikesByDay(from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDateKey()).isEqualTo("2026-11-01");
            assertThat(result.get(0).getCountVal()).isEqualTo(30L);
            verify(postLikeRepository).countLikesByDay(from, to);
        }

        @Test
        @DisplayName("Should delegate to commentRepository.countCommentsByDay")
        void countCommentsByDay_Success() {
            CommentRepository.DateCountProjection proj = mock(CommentRepository.DateCountProjection.class);
            when(proj.getDateKey()).thenReturn("2026-11-01");
            when(proj.getCountVal()).thenReturn(10L);
            when(commentRepository.countCommentsByDay(from, to)).thenReturn(List.of(proj));

            List<CommentRepository.DateCountProjection> result = communityStatsQueryService.countCommentsByDay(from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDateKey()).isEqualTo("2026-11-01");
            assertThat(result.get(0).getCountVal()).isEqualTo(10L);
            verify(commentRepository).countCommentsByDay(from, to);
        }
    }

    @Nested
    @DisplayName("Moderation metrics")
    class ModerationMetricsTests {

        @Test
        @DisplayName("Should delegate to postModerationRepository methods")
        void moderationMetrics_Success() {
            PostModerationRepository.ModerationActionCounts actionCounts =
                    new PostModerationRepository.ModerationActionCounts(50L, 10L, 5L, 3L, 2L, 70L);
            when(postModerationRepository.countModerationActions(from, to)).thenReturn(actionCounts);

            PostModerationRepository.AiLabelCount labelCount =
                    new PostModerationRepository.AiLabelCount("SPAM", 10L);
            when(postModerationRepository.countAiLabels(from, to)).thenReturn(List.of(labelCount));

            when(postModerationRepository.calculateAvgManualReviewDurationSeconds(from, to)).thenReturn(120.5);

            assertThat(communityStatsQueryService.countModerationActions(from, to)).isEqualTo(actionCounts);
            assertThat(communityStatsQueryService.countAiLabels(from, to)).hasSize(1);
            assertThat(communityStatsQueryService.calculateAvgManualReviewDurationSeconds(from, to)).isEqualTo(120.5);

            verify(postModerationRepository).countModerationActions(from, to);
            verify(postModerationRepository).countAiLabels(from, to);
            verify(postModerationRepository).calculateAvgManualReviewDurationSeconds(from, to);
        }
    }
}
