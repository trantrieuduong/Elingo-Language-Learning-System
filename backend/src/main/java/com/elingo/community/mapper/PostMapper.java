package com.elingo.community.mapper;

import com.elingo.community.dto.response.AdminPostModerationDetailResponse;
import com.elingo.community.dto.response.AdminPostModerationResponse;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.dto.response.PostMediaResponse;
import com.elingo.community.dto.response.PostModerationLogResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.Comment;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostMedia;
import com.elingo.community.entity.PostModerationLog;
import com.elingo.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "groupId", source = "post.group.id")
    @Mapping(target = "author", source = "post.user")
    @Mapping(target = "medias", source = "post.medias")
    @Mapping(target = "likedByMe", source = "likedByMe")
    PostResponse toPostResponse(Post post, Boolean likedByMe);

    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "parentCommentId", expression = "java(comment.getParentComment() != null ? comment.getParentComment().getId() : null)")
    @Mapping(target = "author", source = "comment.user")
    @Mapping(target = "replyCount", source = "replyCount")
    @Mapping(target = "likedByMe", source = "likedByMe")
    CommentResponse toCommentResponse(Comment comment, Integer replyCount, Boolean likedByMe);

    PostMediaResponse toPostMediaResponse(PostMedia media);

    List<PostMediaResponse> toPostMediaResponseList(List<PostMedia> medias);

    AuthorResponse toAuthorResponse(User user);

    @Mapping(target = "postId", source = "log.post.id")
    @Mapping(target = "actorId", source = "log.actor.id")
    @Mapping(target = "actorUsername", source = "log.actor.username")
    PostModerationLogResponse toModerationLogResponse(PostModerationLog log);

    @Mapping(target = "id", source = "post.id")
    @Mapping(target = "groupId", source = "post.group.id")
    @Mapping(target = "groupName", source = "post.group.groupName")
    @Mapping(target = "author", source = "post.user")
    @Mapping(target = "medias", source = "medias")
    @Mapping(target = "latestModeration", source = "latestModeration")
    @Mapping(target = "createdAt", source = "post.createdAt")
    @Mapping(target = "updatedAt", source = "post.updatedAt")
    AdminPostModerationResponse toAdminModerationResponse(
            Post post,
            List<PostMediaResponse> medias,
            PostModerationLogResponse latestModeration
    );

    @Mapping(target = "groupId", source = "post.group.id")
    @Mapping(target = "groupName", source = "post.group.groupName")
    @Mapping(target = "author", source = "post.user")
    @Mapping(target = "medias", source = "post.medias")
    @Mapping(target = "moderationLogs", source = "moderationLogs")
    AdminPostModerationDetailResponse toAdminModerationDetailResponse(
            Post post,
            List<PostModerationLogResponse> moderationLogs
    );
}
