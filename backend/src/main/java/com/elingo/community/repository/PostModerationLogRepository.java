package com.elingo.community.repository;

import com.elingo.community.entity.ModerationAction;
import com.elingo.community.entity.ModerationActorType;
import com.elingo.community.entity.PostModerationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostModerationLogRepository extends JpaRepository<PostModerationLog, Long> {

    @Query("""
            SELECT l FROM PostModerationLog l
            LEFT JOIN FETCH l.actor
            WHERE l.post.id = :postId
            ORDER BY l.createdAt DESC, l.id DESC
            """)
    List<PostModerationLog> findByPostIdWithActor(@Param("postId") Long postId);

    @Query("""
            SELECT l FROM PostModerationLog l
            LEFT JOIN FETCH l.actor
            WHERE l.post.id IN :postIds
            ORDER BY l.createdAt DESC, l.id DESC
            """)
    List<PostModerationLog> findByPostIdInWithActor(@Param("postIds") Collection<Long> postIds);

    @Query(value = """
            SELECT l FROM PostModerationLog l
            LEFT JOIN FETCH l.actor
            WHERE (:postId IS NULL OR l.post.id = :postId)
              AND (:actorType IS NULL OR l.actorType = :actorType)
              AND (:action IS NULL OR l.action = :action)
              AND (:aiLabel IS NULL OR l.aiLabel = :aiLabel)
              AND (:createdFrom IS NULL OR l.createdAt >= :createdFrom)
              AND (:createdTo IS NULL OR l.createdAt < :createdTo)
            """,
            countQuery = """
            SELECT COUNT(l) FROM PostModerationLog l
            WHERE (:postId IS NULL OR l.post.id = :postId)
              AND (:actorType IS NULL OR l.actorType = :actorType)
              AND (:action IS NULL OR l.action = :action)
              AND (:aiLabel IS NULL OR l.aiLabel = :aiLabel)
              AND (:createdFrom IS NULL OR l.createdAt >= :createdFrom)
              AND (:createdTo IS NULL OR l.createdAt < :createdTo)
            """)
    Page<PostModerationLog> searchForAdmin(
            @Param("postId") Long postId,
            @Param("actorType") ModerationActorType actorType,
            @Param("action") ModerationAction action,
            @Param("aiLabel") String aiLabel,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable
    );
}
