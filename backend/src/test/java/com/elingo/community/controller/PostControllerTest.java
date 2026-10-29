package com.elingo.community.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.CursorResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.RateLimitRule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.request.CreatePostRequest;
import com.elingo.community.dto.request.UpdatePostRequest;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.dto.response.PostResponse;
import com.elingo.community.entity.PostStatus;
import com.elingo.community.service.CommentService;
import com.elingo.community.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private RateLimiterService limiter;

    @InjectMocks
    private PostController postController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long GROUP_ID = 5L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(CurrentUserId.class);
                            }

                            @Override
                            public Object resolveArgument(@NonNull MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          @NonNull NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return USER_ID;
                            }
                        }
                )
                .build();
    }

    private PostResponse buildPostResponse(Long id, String title) {
        return new PostResponse(
                id,
                GROUP_ID,
                title,
                "Content of post",
                PostStatus.PUBLISHED,
                0,
                0,
                false,
                new AuthorResponse(USER_ID, "user1", "User One", null),
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
        @DisplayName("Create post successfully")
        void createPost_Success() throws Exception {
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "New Post", "This is content", List.of());
            PostResponse response = buildPostResponse(POST_ID, "New Post");

            doNothing().when(limiter).check(RateLimitRule.CREATE_POST, String.valueOf(USER_ID));
            when(postService.createPost(any(CreatePostRequest.class), eq(USER_ID))).thenReturn(response);

            mockMvc.perform(post("/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(POST_ID))
                    .andExpect(jsonPath("$.data.title").value("New Post"));

            verify(limiter).check(RateLimitRule.CREATE_POST, String.valueOf(USER_ID));
            verify(postService).createPost(any(CreatePostRequest.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Create post failed: Rate limit exceeded")
        void createPost_Fail_RateLimitExceeded() throws Exception {
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "New Post", "This is content", List.of());
            doThrow(new AppException(AppError.RATE_LIMIT_EXCEEDED))
                    .when(limiter).check(RateLimitRule.CREATE_POST, String.valueOf(USER_ID));

            mockMvc.perform(post("/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false));

            verify(postService, never()).createPost(any(), any());
        }

        @Test
        @DisplayName("Create post failed: Invalid request body (missing title)")
        void createPost_Fail_MissingTitle() throws Exception {
            CreatePostRequest request = new CreatePostRequest(GROUP_ID, "", "This is content", List.of());

            mockMvc.perform(post("/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(postService, never()).createPost(any(), any());
        }
    }

    // ========================================================================
    // getPublishedPosts
    // ========================================================================
    @Nested
    @DisplayName("getPublishedPosts")
    class GetPublishedPostsTests {

        @Test
        @DisplayName("Get published posts with default parameters")
        void getPublishedPosts_Success_DefaultParams() throws Exception {
            PostResponse post = buildPostResponse(POST_ID, "Post 1");
            CursorResponse<PostResponse> cursorResponse = new CursorResponse<>(List.of(post), "nextCursor123", true);

            when(postService.getPublishedPosts(null, 20, null, USER_ID)).thenReturn(cursorResponse);

            mockMvc.perform(get("/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.nextCursor").value("nextCursor123"))
                    .andExpect(jsonPath("$.data.hasMore").value(true));

            verify(postService).getPublishedPosts(null, 20, null, USER_ID);
        }

        @Test
        @DisplayName("Get published posts with custom query parameters")
        void getPublishedPosts_Success_CustomParams() throws Exception {
            PostResponse post = buildPostResponse(POST_ID, "Group Post");
            CursorResponse<PostResponse> cursorResponse = new CursorResponse<>(List.of(post), null, false);

            when(postService.getPublishedPosts("cursor123", 10, GROUP_ID, USER_ID)).thenReturn(cursorResponse);

            mockMvc.perform(get("/posts")
                            .param("cursor", "cursor123")
                            .param("limit", "10")
                            .param("groupId", String.valueOf(GROUP_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.hasMore").value(false));

            verify(postService).getPublishedPosts("cursor123", 10, GROUP_ID, USER_ID);
        }
    }

    // ========================================================================
    // getPostById
    // ========================================================================
    @Nested
    @DisplayName("getPostById")
    class GetPostByIdTests {

        @Test
        @DisplayName("Get post details by ID successfully")
        void getPostById_Success() throws Exception {
            PostResponse response = buildPostResponse(POST_ID, "Details Post");
            when(postService.getPostById(POST_ID, USER_ID)).thenReturn(response);

            mockMvc.perform(get("/posts/{postId}", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(POST_ID));

            verify(postService).getPostById(POST_ID, USER_ID);
        }

        @Test
        @DisplayName("Get post by ID failed: Post not found")
        void getPostById_Fail_NotFound() throws Exception {
            when(postService.getPostById(POST_ID, USER_ID))
                    .thenThrow(new AppException(AppError.POST_NOT_FOUND));

            mockMvc.perform(get("/posts/{postId}", POST_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Get post by ID failed: Invalid negative ID")
        void getPostById_Fail_NegativeId() throws Exception {
            mockMvc.perform(get("/posts/{postId}", -1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(postService, never()).getPostById(any(), any());
        }
    }

    // ========================================================================
    // updatePost
    // ========================================================================
    @Nested
    @DisplayName("updatePost")
    class UpdatePostTests {

        @Test
        @DisplayName("Update post successfully")
        void updatePost_Success() throws Exception {
            UpdatePostRequest request = new UpdatePostRequest("Updated Title", "Updated Content", List.of());
            PostResponse response = buildPostResponse(POST_ID, "Updated Title");

            when(postService.updatePost(eq(POST_ID), any(UpdatePostRequest.class), eq(USER_ID))).thenReturn(response);

            mockMvc.perform(put("/posts/{postId}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Title"));

            verify(postService).updatePost(eq(POST_ID), any(UpdatePostRequest.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Update post failed: Access denied")
        void updatePost_Fail_AccessDenied() throws Exception {
            UpdatePostRequest request = new UpdatePostRequest("Updated Title", "Updated Content", List.of());
            when(postService.updatePost(eq(POST_ID), any(UpdatePostRequest.class), eq(USER_ID)))
                    .thenThrow(new AppException(AppError.POST_ACCESS_DENIED));

            mockMvc.perform(put("/posts/{postId}", POST_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========================================================================
    // deletePost
    // ========================================================================
    @Nested
    @DisplayName("deletePost")
    class DeletePostTests {

        @Test
        @DisplayName("Delete post successfully")
        void deletePost_Success() throws Exception {
            doNothing().when(postService).deletePost(POST_ID, USER_ID);

            mockMvc.perform(delete("/posts/{postId}", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(postService).deletePost(POST_ID, USER_ID);
        }

        @Test
        @DisplayName("Delete post failed: Access denied")
        void deletePost_Fail_AccessDenied() throws Exception {
            doThrow(new AppException(AppError.POST_ACCESS_DENIED))
                    .when(postService).deletePost(POST_ID, USER_ID);

            mockMvc.perform(delete("/posts/{postId}", POST_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========================================================================
    // getCommentsByPost
    // ========================================================================
    @Nested
    @DisplayName("getCommentsByPost")
    class GetCommentsByPostTests {

        @Test
        @DisplayName("Get comments of post successfully")
        void getCommentsByPost_Success() throws Exception {
            CommentResponse comment = new CommentResponse(
                    100L, POST_ID, null, "Great post!", null, 0, 0, false,
                    new AuthorResponse(USER_ID, "user1", "User One", null),
                    LocalDateTime.now(), LocalDateTime.now()
            );
            Page<CommentResponse> page = new PageImpl<>(List.of(comment), PageRequest.of(0, 20), 1);
            PageResponse<CommentResponse> pageResponse = PageResponse.of(page);

            when(commentService.getCommentsByPost(eq(POST_ID), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/posts/{postId}/comments", POST_ID)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(100));

            verify(commentService).getCommentsByPost(eq(POST_ID), any(Pageable.class), eq(USER_ID));
        }
    }

    // ========================================================================
    // likePost / unlikePost
    // ========================================================================
    @Nested
    @DisplayName("likePost and unlikePost")
    class LikeAndUnlikePostTests {

        @Test
        @DisplayName("Like post successfully")
        void likePost_Success() throws Exception {
            doNothing().when(postService).likePost(POST_ID, USER_ID);

            mockMvc.perform(post("/posts/{postId}/like", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(postService).likePost(POST_ID, USER_ID);
        }

        @Test
        @DisplayName("Like post failed: Already liked")
        void likePost_Fail_AlreadyLiked() throws Exception {
            doThrow(new AppException(AppError.POST_ALREADY_LIKED))
                    .when(postService).likePost(POST_ID, USER_ID);

            mockMvc.perform(post("/posts/{postId}/like", POST_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Unlike post successfully")
        void unlikePost_Success() throws Exception {
            doNothing().when(postService).unlikePost(POST_ID, USER_ID);

            mockMvc.perform(delete("/posts/{postId}/like", POST_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(postService).unlikePost(POST_ID, USER_ID);
        }

        @Test
        @DisplayName("Unlike post failed: Not liked yet")
        void unlikePost_Fail_NotLiked() throws Exception {
            doThrow(new AppException(AppError.POST_NOT_LIKED))
                    .when(postService).unlikePost(POST_ID, USER_ID);

            mockMvc.perform(delete("/posts/{postId}/like", POST_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
