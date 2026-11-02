package com.elingo.community.dto.response;

import com.elingo.community.entity.PostStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostResponse(
        Long id,
        Long groupId,
        String title,
        String content,
        PostStatus status,
        Integer likeCount,
        Integer commentCount,
        Boolean likedByMe,
        AuthorResponse author,
        List<PostMediaResponse> medias,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
