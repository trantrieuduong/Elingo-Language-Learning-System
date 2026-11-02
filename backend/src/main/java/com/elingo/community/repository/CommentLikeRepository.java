package com.elingo.community.repository;

import com.elingo.community.entity.CommentLike;
import com.elingo.community.entity.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

    boolean existsByIdCommentIdAndIdUserId(Long commentId, Long userId);

    void deleteByIdCommentIdAndIdUserId(Long commentId, Long userId);

    @Query("SELECT cl.id.commentId FROM CommentLike cl WHERE cl.id.userId = :userId AND cl.id.commentId IN :commentIds")
    Set<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") Collection<Long> commentIds);
}

