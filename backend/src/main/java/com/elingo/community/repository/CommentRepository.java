package com.elingo.community.repository;

import com.elingo.community.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findByParentCommentId(Long parentCommentId, Pageable pageable);

    long countByParentCommentId(Long parentCommentId);

    @Query("SELECT c.parentComment.id, COUNT(c) FROM Comment c WHERE c.parentComment.id IN :parentIds GROUP BY c.parentComment.id")
    List<Object[]> countRepliesByParentCommentIds(@Param("parentIds") Collection<Long> parentIds);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = CASE WHEN (c.likeCount + :delta) < 0 THEN 0 ELSE (c.likeCount + :delta) END WHERE c.id = :commentId")
    int updateLikeCount(@Param("commentId") Long commentId, @Param("delta") int delta);

    @Query("SELECT c.imageUrl FROM Comment c WHERE c.post.id = :postId AND c.imageUrl IS NOT NULL")
    List<String> findAllImageUrlsByPostId(@Param("postId") Long postId);

    @Query("SELECT c.imageUrl FROM Comment c WHERE (c.id = :commentId OR c.parentComment.id = :commentId) AND c.imageUrl IS NOT NULL")
    List<String> findImageUrlsByCommentOrReplies(@Param("commentId") Long commentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.id = :commentId OR c.parentComment.id = :commentId")
    long countByCommentOrReplies(@Param("commentId") Long commentId);
}
