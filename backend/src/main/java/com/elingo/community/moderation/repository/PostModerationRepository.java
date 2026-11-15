package com.elingo.community.moderation.repository;

import com.elingo.common.enums.MediaKind;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.moderation.model.ModerationResult;
import com.elingo.community.moderation.model.PostForModeration;
import com.elingo.community.moderation.model.PostForModeration.PostMediaItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.elingo.file.util.FileKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dùng JdbcClient thay vì entity: chỉ lấy đúng cột cần
 */
@Repository
public class PostModerationRepository {

    /**
     * Cắt nội dung ngay trong SQL để không kéo cả cột TEXT về.
     */
    private static final int MAX_CONTENT_CHARS = 19_000;

    private final JdbcClient jdbc;
    private final String r2PublicUrl;

    public PostModerationRepository(JdbcClient jdbc,
                                    @Value("${r2.public-url}") String r2PublicUrl) {
        this.jdbc = jdbc;
        this.r2PublicUrl = r2PublicUrl;
    }

    private record PostRow(long id, long userId, String title, String content) {}

    private record MediaRow(long postId, MediaKind kind, String key) {}

    /**
     * Lấy 1 lô bài PENDING kèm media. idx_post_status_time (status, created_at).
     */
    public List<PostForModeration> findPending(int limit) {
        List<PostRow> posts = jdbc.sql("""
                        SELECT id, user_id, title, LEFT(content, :maxContent) AS content
                        FROM posts
                        WHERE status = 'PENDING'
                        ORDER BY created_at, id
                        LIMIT :limit
                        """)
                .param("maxContent", MAX_CONTENT_CHARS)
                .param("limit", limit)
                .query((rs, i) -> new PostRow(
                        rs.getLong("id"), rs.getLong("user_id"), rs.getString("title"), rs.getString("content")))
                .list();
        if (posts.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PostMediaItem>> mediaByPost = findMedia(posts.stream().map(PostRow::id).toList());
        return posts.stream()
                .map(p -> new PostForModeration(p.id(), p.userId(), p.title(), p.content(),
                        mediaByPost.getOrDefault(p.id(), List.of())))
                .toList();
    }

    private Map<Long, List<PostMediaItem>> findMedia(List<Long> postIds) {
        return jdbc.sql("""
                        SELECT post_id, media_type, media_url
                        FROM post_media
                        WHERE post_id IN (:postIds)
                        ORDER BY post_id, `order`, id
                        """)
                .param("postIds", postIds)
                .query((rs, i) -> new MediaRow(
                        rs.getLong("post_id"),
                        MediaKind.valueOf(rs.getString("media_type")),
                        rs.getString("media_url")))
                .list().stream()
                .collect(Collectors.groupingBy(MediaRow::postId,
                        Collectors.mapping(m -> new PostMediaItem(m.kind(), toPublicUrl(m.key())),
                                Collectors.toList())));
    }

    /**
     * Đổi status + ghi log trong 1 transaction ngắn. Chỉ áp dụng khi bài vẫn PENDING
     *
     * @return true nếu đã áp dụng
     */
    @Transactional
    public boolean applyDecision(long postId, ModerationResult result) {
        int updated = jdbc.sql("UPDATE posts SET status = :status WHERE id = :id AND status = 'PENDING'")
                .param("status", statusOf(result.action()))
                .param("id", postId)
                .update();
        if (updated == 0) {
            return false;
        }
        jdbc.sql("""
                INSERT INTO post_moderation_logs (post_id, actor_type, actor_id, action, ai_label, ai_score, note, created_at, updated_at)
                VALUES (:postId, 'AI', NULL, :action, :label, :score, :note, NOW(), NOW())
                """)
                .param("postId", postId)
                .param("action", result.action().name())
                .param("label", truncate(result.label(), 100))
                .param("score", BigDecimal.valueOf(result.score()).setScale(4, RoundingMode.HALF_UP))
                .param("note", truncate(result.note(), 500))
                .update();
        return true;
    }

    private String toPublicUrl(String key) {
        return FileKey.toPublicUrl(r2PublicUrl, key);
    }

    /** Hành động của AI -> posts.status. MANUAL_* thuộc luồng admin nên không được xuất hiện ở đây. */
    private static String statusOf(ModerationAction action) {
        return switch (action) {
            case AUTO_APPROVE -> "PUBLISHED";
            case AUTO_REJECT -> "REJECTED";
            case FLAG_FOR_REVIEW -> "NEEDS_REVIEW";
            case MANUAL_APPROVE, MANUAL_REJECT ->
                    throw new IllegalArgumentException("Moderation never produces manual action: " + action);
        };
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}