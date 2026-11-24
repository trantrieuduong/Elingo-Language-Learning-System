package com.elingo.community.repository;

import com.elingo.community.dto.projection.DateCountProjection;
import com.elingo.community.entity.PostLike;
import com.elingo.community.entity.PostLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {

    boolean existsByIdPostIdAndIdUserId(Long postId, Long userId);

    void deleteByIdPostIdAndIdUserId(Long postId, Long userId);

    @Query("SELECT pl.id.postId FROM PostLike pl WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    Set<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);

    @Query(value = """
            SELECT DATE_FORMAT(created_at, '%Y-%m-%d') AS dateKey, COUNT(*) AS countVal
            FROM post_likes
            WHERE created_at >= :from AND created_at < :to
            GROUP BY dateKey
            ORDER BY dateKey ASC
            """, nativeQuery = true)
    List<DateCountProjection> countLikesByDay(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}

