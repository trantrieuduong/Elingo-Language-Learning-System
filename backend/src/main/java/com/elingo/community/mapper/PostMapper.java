package com.elingo.community.mapper;

import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.dto.response.PostMediaResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.Comment;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostMedia;
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
}
