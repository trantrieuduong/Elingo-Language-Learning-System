package com.elingo.community.service.impl;

import com.elingo.community.dto.projection.DateCountProjection;
import com.elingo.community.dto.projection.PostBacklogProjection;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.projection.PostStatusCountProjection;
import com.elingo.community.moderation.dto.projection.AiLabelCount;
import com.elingo.community.moderation.dto.projection.ModerationActionCounts;
import com.elingo.community.moderation.repository.PostModerationRepository;
import com.elingo.community.repository.CommentRepository;
import com.elingo.community.repository.PostLikeRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.CommunityStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityStatsQueryServiceImpl implements CommunityStatsQueryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final PostModerationRepository postModerationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PostStatusCountProjection> countPostsByStatus() {
        return postRepository.countPostsByStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostGroupCountProjection> countPostsByGroup() {
        return postRepository.countPostsByGroup();
    }

    @Override
    @Transactional(readOnly = true)
    public PostBacklogProjection countPostBacklog() {
        return postRepository.countPostBacklog();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DateCountProjection> countLikesByDay(LocalDateTime from, LocalDateTime to) {
        return postLikeRepository.countLikesByDay(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DateCountProjection> countCommentsByDay(LocalDateTime from, LocalDateTime to) {
        return commentRepository.countCommentsByDay(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationActionCounts countModerationActions(LocalDateTime from, LocalDateTime to) {
        return postModerationRepository.countModerationActions(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiLabelCount> countAiLabels(LocalDateTime from, LocalDateTime to) {
        return postModerationRepository.countAiLabels(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateAvgManualReviewDurationSeconds(LocalDateTime from, LocalDateTime to) {
        return postModerationRepository.calculateAvgManualReviewDurationSeconds(from, to);
    }
}
