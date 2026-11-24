package com.elingo.community.repository;

import com.elingo.community.dto.projection.PostBacklogProjection;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.projection.PostStatusCountProjection;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user u
        JOIN FETCH p.group g
        WHERE p.status = com.elingo.community.entity.PostStatus.PUBLISHED
          AND (
            :cursorCreatedAt IS NULL
            OR (p.createdAt < :cursorCreatedAt)
            OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
          )
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findPublishedPostsByCursor(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user u
        JOIN FETCH p.group g
        WHERE g.id = :groupId
          AND p.status = com.elingo.community.entity.PostStatus.PUBLISHED
          AND (
            :cursorCreatedAt IS NULL
            OR (p.createdAt < :cursorCreatedAt)
            OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
          )
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findPublishedPostsByGroupAndCursor(
            @Param("groupId") Long groupId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.user u
        JOIN FETCH p.group g
        WHERE u.id = :userId
          AND p.status != com.elingo.community.entity.PostStatus.DELETED
          AND (:onlyPublished = false OR p.status = com.elingo.community.entity.PostStatus.PUBLISHED)
          AND (
            :cursorCreatedAt IS NULL
            OR (p.createdAt < :cursorCreatedAt)
            OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)
          )
        ORDER BY p.createdAt DESC, p.id DESC
    """)
    List<Post> findUserPostsByCursor(
            @Param("userId") Long userId,
            @Param("onlyPublished") boolean onlyPublished,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.user JOIN FETCH p.group LEFT JOIN FETCH p.medias WHERE p.id = :postId")
    Optional<Post> findByIdWithMedia(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN (p.likeCount + :delta) < 0 THEN 0 ELSE (p.likeCount + :delta) END WHERE p.id = :postId")
    int updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = CASE WHEN (p.commentCount + :delta) < 0 THEN 0 ELSE (p.commentCount + :delta) END WHERE p.id = :postId")
    int updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Query("select p.user.id from Post p where p.id = :postId and p.status = com.elingo.community.entity.PostStatus.PUBLISHED")
    Optional<Long> findPublishedPostOwnerId(@Param("postId") Long postId);

    @Query("SELECT p.status AS status, COUNT(p.id) AS countVal FROM Post p GROUP BY p.status")
    List<PostStatusCountProjection> countPostsByStatus();

    @Query("""
            SELECT g.id AS groupId, g.groupName AS groupName, COUNT(p.id) AS postCount
            FROM PostGroup g
            LEFT JOIN Post p ON p.group.id = g.id
            GROUP BY g.id, g.groupName
            """)
    List<PostGroupCountProjection> countPostsByGroup();

    @Query(value = """
            SELECT
                COUNT(CASE WHEN status = 'PENDING' THEN 1 END) AS pendingCount,
                COUNT(CASE WHEN status = 'NEEDS_REVIEW' THEN 1 END) AS needsReviewCount
            FROM posts
            """, nativeQuery = true)
    PostBacklogProjection countPostBacklog();
}
