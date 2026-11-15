package com.elingo.community.moderation.service;

import com.elingo.common.event.PostModeratedEvent;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.repository.PostModerationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Điều phối 1 bài: chạy pipeline (ngoài transaction) -> ghi kết quả -> phát event. */
@Slf4j(topic = "POST-MODERATION-SERVICE")
@Service
public class PostModerationService {

    private final PostModerationRepository repository;
    private final ModerationPipeline pipeline;
    private final ApplicationEventPublisher events;

    public PostModerationService(PostModerationRepository repository,
                                 ModerationPipeline pipeline,
                                 ApplicationEventPublisher events) {
        this.repository = repository;
        this.pipeline = pipeline;
        this.events = events;
    }

    public void moderate(PostForModeration post) {
        ModerationResult result = pipeline.run(post);
        apply(post, result);
    }

    /** Bài không kiểm duyệt tự động được: chuyển admin duyệt tay, ghi lý do vào log. */
    public void flagForReview(PostForModeration post, String reason) {
        apply(post, new ModerationResult(ModerationAction.FLAG_FOR_REVIEW, "processing_error", 0.0, reason));
    }

    private void apply(PostForModeration post, ModerationResult result) {
        boolean applied = repository.applyDecision(post.id(), result);
        if (!applied) {
            log.info("Post moderation skipped postId={} reason=NOT_PENDING action={}", post.id(), result.action());
            return;
        }
        log.info("Post moderated postId={} action={} label={} score={} note={}",
                post.id(), result.action(), result.label(), result.score(), result.note());

        events.publishEvent(new PostModeratedEvent(post.id(), post.userId(), result.action()));
    }
}