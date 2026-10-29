package com.elingo.community.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.request.CreateCommentRequest;
import com.elingo.community.dto.request.UpdateCommentRequest;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.entity.Comment;
import com.elingo.community.entity.CommentLike;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.mapper.PostMapper;
import com.elingo.community.repository.CommentLikeRepository;
import com.elingo.community.repository.CommentRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.impl.CommentServiceImpl;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private CommentServiceImpl commentService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 100L;
    private static final Long PARENT_COMMENT_ID = 50L;

    private User testUser;
    private User postAuthor;
    private Post testPost;
    private Comment testComment;
    private Comment parentComment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username("commenter")
                .role(Role.USER)
                .build();

        postAuthor = User.builder()
                .id(OTHER_USER_ID)
                .username("post_author")
                .role(Role.USER)
                .build();

        testPost = Post.builder()
                .id(POST_ID)
                .user(postAuthor)
                .title("Post Title")
                .content("Post Content")
                .status(PostStatus.PUBLISHED)
                .likeCount(0)
                .commentCount(0)
                .build();

        parentComment = Comment.builder()
                .id(PARENT_COMMENT_ID)
                .post(testPost)
                .user(postAuthor)
                .content("Parent comment content")
                .likeCount(0)
                .replies(new ArrayList<>())
                .build();

        testComment = Comment.builder()
                .id(COMMENT_ID)
                .post(testPost)
                .user(testUser)
                .parentComment(null)
                .content("Sample comment")
                .imageUrl("uploads/1/old_image.png")
                .likeCount(0)
                .build();
    }

    private CommentResponse buildCommentResponse(Comment comment, int replyCount, boolean likedByMe) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParentComment() != null ? comment.getParentComment().getId() : null,
                comment.getContent(),
                comment.getImageUrl(),
                comment.getLikeCount(),
                replyCount,
                likedByMe,
                new AuthorResponse(comment.getUser().getId(), comment.getUser().getUsername(), "Name", null),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ========================================================================
    // createComment
    // ========================================================================
    @Nested
    @DisplayName("createComment")
    class CreateCommentTests {

        @Test
        @DisplayName("Create root comment successfully without image")
        void createComment_Success_RootWithoutImage() {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Root comment", null);

            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(COMMENT_ID);
                return c;
            });
            when(postMapper.toCommentResponse(any(Comment.class), eq(0), eq(false)))
                    .thenAnswer(inv -> buildCommentResponse(inv.getArgument(0), 0, false));

            CommentResponse response = commentService.createComment(request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(COMMENT_ID);
            verify(commentRepository).save(any(Comment.class));
            verify(postRepository).updateCommentCount(POST_ID, 1);
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Create root comment successfully with verified image")
        void createComment_Success_WithImage() {
            String imageKey = "verified/1/pic.png";
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Comment with image", imageKey);

            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(COMMENT_ID);
                return c;
            });
            when(postMapper.toCommentResponse(any(Comment.class), eq(0), eq(false)))
                    .thenAnswer(inv -> buildCommentResponse(inv.getArgument(0), 0, false));

            CommentResponse response = commentService.createComment(request, USER_ID);

            assertThat(response).isNotNull();
            verify(commentRepository).save(any(Comment.class));
            verify(postRepository).updateCommentCount(POST_ID, 1);
            verify(publisher).publishEvent(any(FileAttachedEvent.class));
        }

        @Test
        @DisplayName("Create reply comment successfully")
        void createComment_Success_Reply() {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, PARENT_COMMENT_ID, "Reply comment", null);

            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.of(parentComment));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(COMMENT_ID);
                return c;
            });
            when(postMapper.toCommentResponse(any(Comment.class), eq(0), eq(false)))
                    .thenAnswer(inv -> buildCommentResponse(inv.getArgument(0), 0, false));

            CommentResponse response = commentService.createComment(request, USER_ID);

            assertThat(response).isNotNull();
            verify(commentRepository).save(any(Comment.class));
            verify(postRepository).updateCommentCount(POST_ID, 1);
        }

        @Test
        @DisplayName("Create comment failed: Post not found")
        void createComment_Fail_PostNotFound() {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Comment", null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Create comment failed: Post not published")
        void createComment_Fail_PostNotPublished() {
            testPost.setStatus(PostStatus.PENDING);
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Comment", null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> commentService.createComment(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_PUBLISHED));
        }

        @Test
        @DisplayName("Create comment failed: Parent comment not found")
        void createComment_Fail_ParentCommentNotFound() {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, PARENT_COMMENT_ID, "Comment", null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PARENT_COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("Create comment failed: Parent comment belongs to different post")
        void createComment_Fail_ParentBelongsToDifferentPost() {
            Post anotherPost = Post.builder().id(999L).build();
            parentComment.setPost(anotherPost);

            CreateCommentRequest request = new CreateCommentRequest(POST_ID, PARENT_COMMENT_ID, "Comment", null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.of(parentComment));

            assertThatThrownBy(() -> commentService.createComment(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PARENT_COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("Create comment failed: Nesting level exceeded (replying to a reply)")
        void createComment_Fail_NestingLevelExceeded() {
            Comment grandParent = Comment.builder().id(30L).build();
            parentComment.setParentComment(grandParent);

            CreateCommentRequest request = new CreateCommentRequest(POST_ID, PARENT_COMMENT_ID, "Comment", null);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.of(parentComment));

            assertThatThrownBy(() -> commentService.createComment(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_NESTING_LEVEL_EXCEEDED));
        }

        @Test
        @DisplayName("Create comment failed: Image not owned by current user")
        void createComment_Fail_ImageNotOwned() {
            String otherUserImg = "verified/999/pic.png";
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Comment", otherUserImg);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> commentService.createComment(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // getCommentsByPost
    // ========================================================================
    @Nested
    @DisplayName("getCommentsByPost")
    class GetCommentsByPostTests {

        @Test
        @DisplayName("Get comments of post successfully with replies and likes mapped")
        void getCommentsByPost_Success() {
            Page<Comment> commentPage = new PageImpl<>(List.of(testComment), PageRequest.of(0, 20), 1);
            List<Object[]> replyCounts = new ArrayList<>();
            replyCounts.add(new Object[]{COMMENT_ID, 3L});

            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(commentRepository.findByPostIdAndParentCommentIsNull(eq(POST_ID), any(Pageable.class)))
                    .thenReturn(commentPage);
            when(commentRepository.countRepliesByParentCommentIds(any())).thenReturn(replyCounts);
            when(commentLikeRepository.findLikedCommentIds(eq(USER_ID), any())).thenReturn(Set.of(COMMENT_ID));
            when(postMapper.toCommentResponse(eq(testComment), eq(3), eq(true)))
                    .thenReturn(buildCommentResponse(testComment, 3, true));

            PageResponse<CommentResponse> result = commentService.getCommentsByPost(POST_ID, PageRequest.of(0, 20), USER_ID);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).replyCount()).isEqualTo(3);
            assertThat(result.content().get(0).likedByMe()).isTrue();
        }

        @Test
        @DisplayName("Get comments of post successfully when empty page")
        void getCommentsByPost_Success_Empty() {
            Page<Comment> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(commentRepository.findByPostIdAndParentCommentIsNull(eq(POST_ID), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<CommentResponse> result = commentService.getCommentsByPost(POST_ID, PageRequest.of(0, 20), USER_ID);

            assertThat(result.content()).isEmpty();
            verify(commentRepository, never()).countRepliesByParentCommentIds(any());
        }

        @Test
        @DisplayName("Get comments: Author can view comments of pending post")
        void getCommentsByPost_Success_AuthorCanViewPending() {
            testPost.setStatus(PostStatus.PENDING);
            Page<Comment> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(commentRepository.findByPostIdAndParentCommentIsNull(eq(POST_ID), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<CommentResponse> result = commentService.getCommentsByPost(POST_ID, PageRequest.of(0, 20), OTHER_USER_ID);

            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("Get comments failed: Other user cannot view comments of pending post")
        void getCommentsByPost_Fail_OtherUserPending() {
            testPost.setStatus(PostStatus.PENDING);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> commentService.getCommentsByPost(POST_ID, PageRequest.of(0, 20), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }
    }

    // ========================================================================
    // getRepliesByComment
    // ========================================================================
    @Nested
    @DisplayName("getRepliesByComment")
    class GetRepliesByCommentTests {

        @Test
        @DisplayName("Get replies of comment successfully")
        void getRepliesByComment_Success() {
            Page<Comment> repliesPage = new PageImpl<>(List.of(testComment), PageRequest.of(0, 20), 1);

            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.of(parentComment));
            when(commentRepository.findByParentCommentId(eq(PARENT_COMMENT_ID), any(Pageable.class)))
                    .thenReturn(repliesPage);
            when(commentLikeRepository.findLikedCommentIds(eq(USER_ID), any())).thenReturn(Set.of());
            when(postMapper.toCommentResponse(eq(testComment), eq(0), eq(false)))
                    .thenReturn(buildCommentResponse(testComment, 0, false));

            PageResponse<CommentResponse> result = commentService.getRepliesByComment(PARENT_COMMENT_ID, PageRequest.of(0, 20), USER_ID);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("Get replies of comment successfully when empty")
        void getRepliesByComment_Success_Empty() {
            Page<Comment> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.of(parentComment));
            when(commentRepository.findByParentCommentId(eq(PARENT_COMMENT_ID), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResponse<CommentResponse> result = commentService.getRepliesByComment(PARENT_COMMENT_ID, PageRequest.of(0, 20), USER_ID);

            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("Get replies failed: Parent comment not found")
        void getRepliesByComment_Fail_ParentNotFound() {
            when(commentRepository.findById(PARENT_COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.getRepliesByComment(PARENT_COMMENT_ID, PageRequest.of(0, 20), USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.PARENT_COMMENT_NOT_FOUND));
        }
    }

    // ========================================================================
    // updateComment
    // ========================================================================
    @Nested
    @DisplayName("updateComment")
    class UpdateCommentTests {

        @Test
        @DisplayName("Update comment successfully: Change content and upload new verified image")
        void updateComment_Success_NewImage() {
            String newVerifiedKey = "verified/1/new_pic.png";
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", newVerifiedKey);

            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentRepository.countByParentCommentId(COMMENT_ID)).thenReturn(2L);
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(false);
            when(postMapper.toCommentResponse(eq(testComment), eq(2), eq(false)))
                    .thenAnswer(inv -> buildCommentResponse(testComment, 2, false));

            CommentResponse response = commentService.updateComment(COMMENT_ID, request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(testComment.getContent()).isEqualTo("Updated content");
            assertThat(testComment.getImageUrl()).isEqualTo("uploads/1/new_pic.png");

            verify(publisher).publishEvent(any(FileAttachedEvent.class));
            verify(publisher).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Update comment successfully: Keep existing uploads image")
        void updateComment_Success_KeepExistingImage() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", "uploads/1/old_image.png");

            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentRepository.countByParentCommentId(COMMENT_ID)).thenReturn(0L);
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(false);
            when(postMapper.toCommentResponse(eq(testComment), eq(0), eq(false)))
                    .thenAnswer(inv -> buildCommentResponse(testComment, 0, false));

            CommentResponse response = commentService.updateComment(COMMENT_ID, request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(testComment.getImageUrl()).isEqualTo("uploads/1/old_image.png");
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Update comment successfully: Remove old image")
        void updateComment_Success_RemoveImage() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", null);

            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentRepository.countByParentCommentId(COMMENT_ID)).thenReturn(0L);
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(false);
            when(postMapper.toCommentResponse(eq(testComment), eq(0), eq(false)))
                    .thenAnswer(inv -> buildCommentResponse(testComment, 0, false));

            CommentResponse response = commentService.updateComment(COMMENT_ID, request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(testComment.getImageUrl()).isNull();
            verify(publisher).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Update comment failed: Comment not found")
        void updateComment_Fail_NotFound() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", null);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("Update comment failed: Access denied")
        void updateComment_Fail_AccessDenied() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", null);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, request, OTHER_USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_ACCESS_DENIED));
        }

        @Test
        @DisplayName("Update comment failed: Post not published")
        void updateComment_Fail_PostNotPublished() {
            testPost.setStatus(PostStatus.PENDING);
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", null);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_PUBLISHED));
        }

        @Test
        @DisplayName("Update comment failed: Different uploads image passed")
        void updateComment_Fail_DifferentUploadsImage() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", "uploads/1/other.png");
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("Update comment failed: Invalid zone for image (e.g. staging)")
        void updateComment_Fail_InvalidZone() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", "staging/1/temp.png");
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("Update comment failed: New image not owned by user")
        void updateComment_Fail_ImageNotOwned() {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", "verified/999/other.png");
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.updateComment(COMMENT_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // deleteComment
    // ========================================================================
    @Nested
    @DisplayName("deleteComment")
    class DeleteCommentTests {

        @Test
        @DisplayName("Delete comment successfully: By comment author")
        void deleteComment_Success_ByCommentAuthor() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentRepository.countByCommentOrReplies(COMMENT_ID)).thenReturn(2L);
            when(commentRepository.findImageUrlsByCommentOrReplies(COMMENT_ID))
                    .thenReturn(List.of("uploads/1/old_image.png"));

            commentService.deleteComment(COMMENT_ID, USER_ID);

            verify(commentRepository).delete(testComment);
            verify(postRepository).updateCommentCount(POST_ID, -2);
            verify(publisher).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Delete comment successfully: By post author (moderating own post)")
        void deleteComment_Success_ByPostAuthor() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentRepository.countByCommentOrReplies(COMMENT_ID)).thenReturn(1L);
            when(commentRepository.findImageUrlsByCommentOrReplies(COMMENT_ID)).thenReturn(List.of());

            commentService.deleteComment(COMMENT_ID, OTHER_USER_ID);

            verify(commentRepository).delete(testComment);
            verify(postRepository).updateCommentCount(POST_ID, -1);
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Delete comment failed: Comment not found")
        void deleteComment_Fail_NotFound() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("Delete comment failed: Access denied (neither comment author nor post author)")
        void deleteComment_Fail_AccessDenied() {
            Long strangerId = 99L;
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.deleteComment(COMMENT_ID, strangerId))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // likeComment
    // ========================================================================
    @Nested
    @DisplayName("likeComment")
    class LikeCommentTests {

        @Test
        @DisplayName("Like comment successfully")
        void likeComment_Success() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            commentService.likeComment(COMMENT_ID, USER_ID);

            verify(commentLikeRepository).save(any(CommentLike.class));
            verify(commentRepository).updateLikeCount(COMMENT_ID, 1);
        }

        @Test
        @DisplayName("Like comment failed: Comment not found")
        void likeComment_Fail_NotFound() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.likeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("Like comment failed: Post not published")
        void likeComment_Fail_PostNotPublished() {
            testPost.setStatus(PostStatus.PENDING);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.likeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_PUBLISHED));
        }

        @Test
        @DisplayName("Like comment failed: Already liked")
        void likeComment_Fail_AlreadyLiked() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> commentService.likeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_ALREADY_LIKED));
        }

        @Test
        @DisplayName("Like comment failed: User not found")
        void likeComment_Fail_UserNotFound() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.likeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }
    }

    // ========================================================================
    // unlikeComment
    // ========================================================================
    @Nested
    @DisplayName("unlikeComment")
    class UnlikeCommentTests {

        @Test
        @DisplayName("Unlike comment successfully")
        void unlikeComment_Success() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(true);

            commentService.unlikeComment(COMMENT_ID, USER_ID);

            verify(commentLikeRepository).deleteByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID);
            verify(commentRepository).updateLikeCount(COMMENT_ID, -1);
        }

        @Test
        @DisplayName("Unlike comment failed: Comment not found")
        void unlikeComment_Fail_NotFound() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.unlikeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("Unlike comment failed: Post not published")
        void unlikeComment_Fail_PostNotPublished() {
            testPost.setStatus(PostStatus.PENDING);
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));

            assertThatThrownBy(() -> commentService.unlikeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_PUBLISHED));
        }

        @Test
        @DisplayName("Unlike comment failed: Not liked yet")
        void unlikeComment_Fail_NotLiked() {
            when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(testComment));
            when(commentLikeRepository.existsByIdCommentIdAndIdUserId(COMMENT_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> commentService.unlikeComment(COMMENT_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.COMMENT_NOT_LIKED));
        }
    }
}
