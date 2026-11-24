package com.elingo.community.service;

import com.elingo.community.dto.projection.DateCountProjection;
import com.elingo.community.dto.projection.PostBacklogProjection;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.projection.PostStatusCountProjection;
import com.elingo.community.moderation.dto.projection.AiLabelCount;
import com.elingo.community.moderation.dto.projection.ModerationActionCounts;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunityStatsQueryService {
    List<PostStatusCountProjection> countPostsByStatus();
    List<PostGroupCountProjection> countPostsByGroup();
    PostBacklogProjection countPostBacklog();
    List<DateCountProjection> countLikesByDay(LocalDateTime from, LocalDateTime to);
    List<DateCountProjection> countCommentsByDay(LocalDateTime from, LocalDateTime to);
    ModerationActionCounts countModerationActions(LocalDateTime from, LocalDateTime to);
    List<AiLabelCount> countAiLabels(LocalDateTime from, LocalDateTime to);
    Double calculateAvgManualReviewDurationSeconds(LocalDateTime from, LocalDateTime to);
}
