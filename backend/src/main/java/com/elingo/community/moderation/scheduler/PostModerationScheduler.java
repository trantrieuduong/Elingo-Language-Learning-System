package com.elingo.community.moderation.scheduler;

import com.elingo.community.moderation.config.ModerationProperties;
import com.elingo.community.moderation.exception.ModerationUnavailableException;
import com.elingo.community.moderation.exception.UnprocessableContentException;
import com.elingo.community.moderation.model.PostForModeration;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.elingo.community.moderation.repository.PostModerationRepository;
import com.elingo.community.moderation.service.PostModerationService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j(topic = "POST-MODERATION-SCHEDULER")
@Component
@ConditionalOnProperty(prefix = "app.community.moderation.scheduler", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class PostModerationScheduler {

    /** Thời gian tạm dừng khi moderation-service/OpenAI/ffmpeg không khả dụng. */
    private static final Duration UNAVAILABLE_PAUSE = Duration.ofSeconds(60);
    private static final int MAX_NOTE_LENGTH = 400;

    private final PostModerationRepository repository;
    private final PostModerationService service;
    private final ModerationProperties.Scheduler cfg;

    /** Chỉ truy cập từ luồng scheduler (fixedDelay nên các lượt chạy không chồng nhau). */
    private final Map<Long, Integer> failures = new HashMap<>();
    private volatile Instant pausedUntil = Instant.EPOCH;

    public PostModerationScheduler(PostModerationRepository repository,
                                   PostModerationService service,
                                   ModerationProperties props) {
        this.repository = repository;
        this.service = service;
        this.cfg = props.scheduler();
    }

    @Scheduled(fixedDelayString = "${app.community.moderation.scheduler.fixed-delay-seconds:30}",
            initialDelayString = "${app.community.moderation.scheduler.fixed-delay-seconds:30}",
            timeUnit = TimeUnit.SECONDS)
    public void moderatePendingPosts() {
        if (Instant.now().isBefore(pausedUntil)) {
            return;
        }
        List<PostForModeration> batch = repository.findPending(cfg.batchSize());
        if (batch.isEmpty()) {
            return;
        }

        log.info("Moderation batch started batchSize={}", batch.size());
        for (PostForModeration post : batch) {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", "moderation-" + post.id())) {
                if (!process(post)) {
                    return; // provider đang lỗi: bỏ phần còn lại của lô
                }
            }
        }
    }

    /** @return false nếu cần dừng cả lô */
    private boolean process(PostForModeration post) {
        try {
            service.moderate(post);
            failures.remove(post.id());
            return true;
        } catch (ModerationUnavailableException e) {
            // Lỗi hạ tầng không phải lỗi của bài nên không tính vào số lần thử, chỉ tạm dừng scheduler
            pausedUntil = Instant.now().plus(UNAVAILABLE_PAUSE);
            log.warn("Moderation provider unavailable pauseDuration={}", UNAVAILABLE_PAUSE, e);
            return false;
        } catch (UnprocessableContentException e) {
            log.warn("Post moderation unprocessable postId={} reason={}", post.id(), e.getMessage());
            failures.remove(post.id());
            service.flagForReview(post, truncate(e.getMessage()));
            return true;
        } catch (RuntimeException e) {
            int attempts = failures.merge(post.id(), 1, Integer::sum);
            log.error("Post moderation failed postId={} attempt={} maxAttempts={}", post.id(), attempts, cfg.maxAttempts(), e);
            if (attempts >= cfg.maxAttempts()) {
                failures.remove(post.id());
                service.flagForReview(post, "Processing error: " + e.getClass().getSimpleName());
            }
            return true;
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return "Unprocessable content";
        }
        return s.length() > MAX_NOTE_LENGTH ? s.substring(0, MAX_NOTE_LENGTH) : s;
    }
}