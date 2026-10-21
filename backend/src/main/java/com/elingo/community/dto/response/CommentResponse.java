package com.elingo.community.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String content,
        @BuildMediaUrl String imageUrl,
        Integer likeCount,
        Integer replyCount,
        Boolean likedByMe,
        AuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
