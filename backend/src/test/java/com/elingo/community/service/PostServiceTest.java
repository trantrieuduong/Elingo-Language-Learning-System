package com.elingo.community.service;

import com.elingo.common.dto.CursorResponse;
import com.elingo.common.event.FileAttachedEvent;
import com.elingo.common.event.FileDeletedEvent;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.util.CursorUtils;
import com.elingo.community.dto.request.CreatePostRequest;
import com.elingo.community.dto.request.UpdatePostRequest;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.PostMediaResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.MediaType;
import com.elingo.community.entity.Post;
import com.elingo.community.entity.PostGroup;
import com.elingo.community.entity.PostLike;
import com.elingo.community.entity.PostLikeId;
import com.elingo.community.entity.PostMedia;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.mapper.PostMapper;
import com.elingo.community.repository.PostGroupRepository;
import com.elingo.community.repository.PostLikeRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.impl.PostServiceImpl;
import com.elingo.user.entity.Role;
import com.elingo.user.entity.User;
import com.elingo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostGroupRepository postGroupRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostMapper postMapper;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private PostServiceImpl postService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long GROUP_ID = 5L;

    private User testUser;
    private PostGroup testGroup;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .username("testuser")
                .email("test@gmail.com")
                .role(Role.USER)
                .build();

        testGroup = PostGroup.builder()
                .id(GROUP_ID)
                .groupName("English Daily")
                .description("Daily discussions")
                .build();

        testPost = Post.builder()
                .id(POST_ID)
                .user(testUser)
                .group(testGroup)
                .title("Hello World")
                .content("Learning English today")
                .status(PostStatus.PUBLISHED)
                .likeCount(0)
                .commentCount(0)
                .medias(new ArrayList<>())
                .build();
    }

    private PostResponse buildPostResponse(Post post, boolean likedByMe) {
        return new PostResponse(
                post.getId(),
                post.getGroup() != null ? post.getGroup().getId() : null,
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByMe,
                new AuthorResponse(post.getUser().getId(), post.getUser().getUsername(), "Name", null),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ========================================================================
    // createPost
    // ========================================================================
    @Nested
    @DisplayName("createPost")
    class CreatePostTests {

        @Test
        @DisplayName("Create post successfully without media")
        void createPost_Success_NoMedia() {
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "Title", "Content", List.of());
            when(postGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post p = invocation.getArgument(0);
                p.setId(POST_ID);
                return p;
            });
            when(postMapper.toPostResponse(any(Post.class), eq(false)))
                    .thenAnswer(inv -> buildPostResponse(inv.getArgument(0), false));

            PostResponse response = postService.createPost(request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(POST_ID);
            verify(postRepository).save(any(Post.class));
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Create post successfully with valid image and video media")
        void createPost_Success_WithMedia() {
            String imgKey = "verified/1/image.png";
            String vidKey = "verified/1/video.mp4";
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "Title", "Content", List.of(imgKey, vidKey));

            when(postGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post p = invocation.getArgument(0);
                p.setId(POST_ID);
                return p;
            });
            when(postMapper.toPostResponse(any(Post.class), eq(false)))
                    .thenAnswer(inv -> buildPostResponse(inv.getArgument(0), false));

            PostResponse response = postService.createPost(request, USER_ID);

            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
            verify(publisher, times(2)).publishEvent(any(FileAttachedEvent.class));
        }

        @Test
        @DisplayName("Create post failed: Post group not found")
        void createPost_Fail_GroupNotFound() {
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "Title", "Content", List.of());
            when(postGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.createPost(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_NOT_FOUND));

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create post failed: User not found")
        void createPost_Fail_UserNotFound() {
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "Title", "Content", List.of());
            when(postGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.createPost(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create post failed: Media not owned by current user (IDOR prevention)")
        void createPost_Fail_MediaNotOwned() {
            String otherUserKey = "verified/999/image.png";
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "Title", "Content", List.of(otherUserKey));
            when(postGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post p = invocation.getArgument(0);
                p.setId(POST_ID);
                return p;
            });

            assertThatThrownBy(() -> postService.createPost(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
        }

        @Test
        @DisplayName("Create post failed: Invalid media kind (e.g. audio/pdf)")
        void createPost_Fail_InvalidMediaKind() {
            String audioKey = "verified/1/sound.mp3";
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "Title", "Content", List.of(audioKey));
            when(postGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(testGroup));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post p = invocation.getArgument(0);
                p.setId(POST_ID);
                return p;
            });

            assertThatThrownBy(() -> postService.createPost(request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_FILE_TYPE));
        }
    }

    // ========================================================================
    // getPublishedPosts
    // ========================================================================
    @Nested
    @DisplayName("getPublishedPosts")
    class GetPublishedPostsTests {

        @Test
        @DisplayName("Get published posts without cursor or groupId")
        void getPublishedPosts_Success_NoCursorNoGroup() {
            when(postRepository.findPublishedPostsByCursor(any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(testPost));
            when(postLikeRepository.findLikedPostIds(eq(USER_ID), any()))
                    .thenReturn(Set.of(POST_ID));
            when(postMapper.toPostResponse(eq(testPost), eq(true)))
                    .thenReturn(buildPostResponse(testPost, true));

            CursorResponse<PostResponse> result = postService.getPublishedPosts(null, 20, null, USER_ID);

            assertThat(result.items()).hasSize(1);
            assertThat(result.hasMore()).isFalse();
            verify(postRepository).findPublishedPostsByCursor(any(), any(), any(Pageable.class));
            verify(postLikeRepository).findLikedPostIds(eq(USER_ID), any());
        }

        @Test
        @DisplayName("Get published posts with valid cursor and groupId")
        void getPublishedPosts_Success_WithCursorAndGroup() {
            LocalDateTime now = LocalDateTime.now();
            String cursor = CursorUtils.encode(now, 100L);

            when(postRepository.findPublishedPostsByGroupAndCursor(eq(GROUP_ID), any(), eq(100L), any(Pageable.class)))
                    .thenReturn(List.of());

            CursorResponse<PostResponse> result = postService.getPublishedPosts(cursor, 10, GROUP_ID, null);

            assertThat(result.items()).isEmpty();
            assertThat(result.hasMore()).isFalse();
            verify(postRepository).findPublishedPostsByGroupAndCursor(eq(GROUP_ID), any(), eq(100L), any(Pageable.class));
            verify(postLikeRepository, never()).findLikedPostIds(any(), any());
        }

        @Test
        @DisplayName("Get published posts with clamped limits")
        void getPublishedPosts_Success_ClampLimits() {
            when(postRepository.findPublishedPostsByCursor(any(), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            // Limit quá lớn (100) sẽ clamp về 50 -> pageRequest size là 51
            postService.getPublishedPosts(null, 100, null, null);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(postRepository).findPublishedPostsByCursor(any(), any(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(51);
        }
    }

    // ========================================================================
    // getUserPosts
    // ========================================================================
    @Nested
    @DisplayName("getUserPosts")
    class GetUserPostsTests {

        @Test
        @DisplayName("Get user posts: Current user views own posts (include pending)")
        void getUserPosts_Success_OwnPosts() {
            when(postRepository.findUserPostsByCursor(eq(USER_ID), eq(false), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(testPost));
            when(postLikeRepository.findLikedPostIds(eq(USER_ID), any())).thenReturn(Set.of());
            when(postMapper.toPostResponse(eq(testPost), eq(false))).thenReturn(buildPostResponse(testPost, false));

            CursorResponse<PostResponse> result = postService.getUserPosts(USER_ID, null, 20, USER_ID);

            assertThat(result.items()).hasSize(1);
            verify(postRepository).findUserPostsByCursor(eq(USER_ID), eq(false), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Get user posts: Viewing another user's posts (only published)")
        void getUserPosts_Success_OtherUserPosts() {
            when(postRepository.findUserPostsByCursor(eq(OTHER_USER_ID), eq(true), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(testPost));
            when(postLikeRepository.findLikedPostIds(eq(USER_ID), any())).thenReturn(Set.of());
            when(postMapper.toPostResponse(eq(testPost), eq(false))).thenReturn(buildPostResponse(testPost, false));

            CursorResponse<PostResponse> result = postService.getUserPosts(OTHER_USER_ID, null, 20, USER_ID);

            assertThat(result.items()).hasSize(1);
            verify(postRepository).findUserPostsByCursor(eq(OTHER_USER_ID), eq(true), any(), any(), any(Pageable.class));
        }
    }

    // ========================================================================
    // getPostById
    // ========================================================================
    @Nested
    @DisplayName("getPostById")
    class GetPostByIdTests {

        @Test
        @DisplayName("Get published post by ID successfully")
        void getPostById_Success_Published() {
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(true);
            when(postMapper.toPostResponse(testPost, true)).thenReturn(buildPostResponse(testPost, true));

            PostResponse response = postService.getPostById(POST_ID, USER_ID);

            assertThat(response).isNotNull();
            assertThat(response.likedByMe()).isTrue();
            verify(postRepository).findByIdWithMedia(POST_ID);
        }

        @Test
        @DisplayName("Get pending post by ID: Author can view")
        void getPostById_Success_Pending_AuthorCanView() {
            testPost.setStatus(PostStatus.PENDING);
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(false);
            when(postMapper.toPostResponse(testPost, false)).thenReturn(buildPostResponse(testPost, false));

            PostResponse response = postService.getPostById(POST_ID, USER_ID);

            assertThat(response).isNotNull();
            verify(postRepository).findByIdWithMedia(POST_ID);
        }

        @Test
        @DisplayName("Get post by ID failed: Post not found")
        void getPostById_Fail_NotFound() {
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPostById(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Get post by ID failed: Post is soft deleted")
        void getPostById_Fail_Deleted() {
            testPost.setStatus(PostStatus.DELETED);
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.getPostById(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Get pending post by ID failed: Other user cannot view")
        void getPostById_Fail_Pending_OtherUserCannotView() {
            testPost.setStatus(PostStatus.PENDING);
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.getPostById(POST_ID, OTHER_USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }
    }

    // ========================================================================
    // updatePost
    // ========================================================================
    @Nested
    @DisplayName("updatePost")
    class UpdatePostTests {

        @Test
        @DisplayName("Update post successfully: Retain existing media, add new media, remove old media")
        void updatePost_Success_SyncMedias() {
            PostMedia mediaA = PostMedia.builder()
                    .id(1L)
                    .post(testPost)
                    .mediaUrl("uploads/1/a.png")
                    .mediaType(MediaType.IMAGE)
                    .order(0)
                    .build();
            PostMedia mediaB = PostMedia.builder()
                    .id(2L)
                    .post(testPost)
                    .mediaUrl("uploads/1/b.png")
                    .mediaType(MediaType.IMAGE)
                    .order(1)
                    .build();
            testPost.getMedias().add(mediaA);
            testPost.getMedias().add(mediaB);

            // Giữ mediaA, thêm newKey, loại bỏ mediaB
            String newKey = "verified/1/c.png";
            UpdatePostRequest request = new UpdatePostRequest("Updated Title", "Updated Content", List.of("uploads/1/a.png", newKey));

            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(false);
            when(postMapper.toPostResponse(eq(testPost), eq(false))).thenAnswer(inv -> buildPostResponse(testPost, false));

            PostResponse response = postService.updatePost(POST_ID, request, USER_ID);

            assertThat(response).isNotNull();
            assertThat(testPost.getTitle()).isEqualTo("Updated Title");
            assertThat(testPost.getStatus()).isEqualTo(PostStatus.PENDING);
            assertThat(testPost.getMedias()).hasSize(2);
            assertThat(mediaA.getOrder()).isEqualTo(0);

            // Kiểm tra event: attach cho newKey, delete cho mediaB
            verify(publisher).publishEvent(any(FileAttachedEvent.class));
            verify(publisher).publishEvent(any(FileDeletedEvent.class));
        }

        @Test
        @DisplayName("Update post failed: Post not found")
        void updatePost_Fail_NotFound() {
            UpdatePostRequest request = new UpdatePostRequest("Title", "Content", List.of());
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(POST_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Update post failed: Post is deleted")
        void updatePost_Fail_Deleted() {
            testPost.setStatus(PostStatus.DELETED);
            UpdatePostRequest request = new UpdatePostRequest("Title", "Content", List.of());
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.updatePost(POST_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Update post failed: Access denied (not author)")
        void updatePost_Fail_AccessDenied() {
            UpdatePostRequest request = new UpdatePostRequest("Title", "Content", List.of());
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.updatePost(POST_ID, request, OTHER_USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_ACCESS_DENIED));
        }

        @Test
        @DisplayName("Update post failed: Provided uploads key not in existing medias")
        void updatePost_Fail_UploadKeyNotExisting() {
            UpdatePostRequest request = new UpdatePostRequest("Title", "Content", List.of("uploads/1/unknown.png"));
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.updatePost(POST_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_REQUEST));
        }

        @Test
        @DisplayName("Update post failed: New verified media not owned by user")
        void updatePost_Fail_NewMediaNotOwned() {
            UpdatePostRequest request = new UpdatePostRequest("Title", "Content", List.of("verified/999/other.png"));
            when(postRepository.findByIdWithMedia(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.updatePost(POST_ID, request, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.FILE_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // deletePost
    // ========================================================================
    @Nested
    @DisplayName("deletePost")
    class DeletePostTests {

        @Test
        @DisplayName("Delete post successfully (soft delete)")
        void deletePost_Success() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            postService.deletePost(POST_ID, USER_ID);

            assertThat(testPost.getStatus()).isEqualTo(PostStatus.DELETED);
            verify(publisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Delete post failed: Post not found")
        void deletePost_Fail_NotFound() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.deletePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Delete post failed: Already deleted")
        void deletePost_Fail_AlreadyDeleted() {
            testPost.setStatus(PostStatus.DELETED);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.deletePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Delete post failed: Access denied")
        void deletePost_Fail_AccessDenied() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.deletePost(POST_ID, OTHER_USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_ACCESS_DENIED));
        }
    }

    // ========================================================================
    // likePost
    // ========================================================================
    @Nested
    @DisplayName("likePost")
    class LikePostTests {

        @Test
        @DisplayName("Like post successfully")
        void likePost_Success() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            postService.likePost(POST_ID, USER_ID);

            verify(postLikeRepository).save(any(PostLike.class));
            verify(postRepository).updateLikeCount(POST_ID, 1);
        }

        @Test
        @DisplayName("Like post failed: Post not found")
        void likePost_Fail_NotFound() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Like post failed: Post not published")
        void likePost_Fail_NotPublished() {
            testPost.setStatus(PostStatus.PENDING);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_PUBLISHED));
        }

        @Test
        @DisplayName("Like post failed: Already liked")
        void likePost_Fail_AlreadyLiked() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> postService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_ALREADY_LIKED));
        }

        @Test
        @DisplayName("Like post failed: User not found")
        void likePost_Fail_UserNotFound() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.likePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.USER_NOT_FOUND));
        }
    }

    // ========================================================================
    // unlikePost
    // ========================================================================
    @Nested
    @DisplayName("unlikePost")
    class UnlikePostTests {

        @Test
        @DisplayName("Unlike post successfully")
        void unlikePost_Success() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(true);

            postService.unlikePost(POST_ID, USER_ID);

            verify(postLikeRepository).deleteByIdPostIdAndIdUserId(POST_ID, USER_ID);
            verify(postRepository).updateLikeCount(POST_ID, -1);
        }

        @Test
        @DisplayName("Unlike post failed: Post not found")
        void unlikePost_Fail_NotFound() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.unlikePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Unlike post failed: Post is deleted")
        void unlikePost_Fail_Deleted() {
            testPost.setStatus(PostStatus.DELETED);
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.unlikePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }

        @Test
        @DisplayName("Unlike post failed: Not liked yet")
        void unlikePost_Fail_NotLiked() {
            when(postRepository.findById(POST_ID)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByIdPostIdAndIdUserId(POST_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> postService.unlikePost(POST_ID, USER_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_LIKED));
        }
    }
}
