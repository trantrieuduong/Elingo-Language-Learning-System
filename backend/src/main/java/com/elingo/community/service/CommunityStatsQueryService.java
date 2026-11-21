package com.elingo.community.service;

import com.elingo.community.moderation.repository.PostModerationRepository;
import com.elingo.community.repository.CommentRepository;
import com.elingo.community.repository.PostLikeRepository;
import com.elingo.community.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunityStatsQueryService {
    List<PostRepository.PostStatusCountProjection> countPostsByStatus();
    List<PostRepository.PostGroupCountProjection> countPostsByGroup();
    PostRepository.PostBacklogProjection countPostBacklog();
    List<PostLikeRepository.DateCountProjection> countLikesByDay(LocalDateTime from, LocalDateTime to);
    List<CommentRepository.DateCountProjection> countCommentsByDay(LocalDateTime from, LocalDateTime to);
    PostModerationRepository.ModerationActionCounts countModerationActions(LocalDateTime from, LocalDateTime to);
    List<PostModerationRepository.AiLabelCount> countAiLabels(LocalDateTime from, LocalDateTime to);
    Double calculateAvgManualReviewDurationSeconds(LocalDateTime from, LocalDateTime to);
}
