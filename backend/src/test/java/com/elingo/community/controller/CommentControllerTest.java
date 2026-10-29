package com.elingo.community.controller;

import com.elingo.common.annotation.CurrentUserId;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.enums.RateLimitRule;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.common.service.RateLimiterService;
import com.elingo.community.dto.request.CreateCommentRequest;
import com.elingo.community.dto.request.UpdateCommentRequest;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.CommentResponse;
import com.elingo.community.service.CommentService;
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
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommentService commentService;

    @Mock
    private RateLimiterService limiter;

    @InjectMocks
    private CommentController commentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long USER_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 100L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(commentController)
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

    private CommentResponse buildCommentResponse(Long id, Long parentId, String content) {
        return new CommentResponse(
                id,
                POST_ID,
                parentId,
                content,
                null,
                0,
                0,
                false,
                new AuthorResponse(USER_ID, "user1", "User One", null),
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
        @DisplayName("Create comment successfully")
        void createComment_Success() throws Exception {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Hello comment", null);
            CommentResponse response = buildCommentResponse(COMMENT_ID, null, "Hello comment");

            doNothing().when(limiter).check(RateLimitRule.COMMENT_POST, String.valueOf(USER_ID));
            when(commentService.createComment(any(CreateCommentRequest.class), eq(USER_ID))).thenReturn(response);

            mockMvc.perform(post("/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(COMMENT_ID))
                    .andExpect(jsonPath("$.data.content").value("Hello comment"));

            verify(limiter).check(RateLimitRule.COMMENT_POST, String.valueOf(USER_ID));
            verify(commentService).createComment(any(CreateCommentRequest.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Create comment failed: Rate limit exceeded")
        void createComment_Fail_RateLimitExceeded() throws Exception {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "Hello comment", null);
            doThrow(new AppException(AppError.RATE_LIMIT_EXCEEDED))
                    .when(limiter).check(RateLimitRule.COMMENT_POST, String.valueOf(USER_ID));

            mockMvc.perform(post("/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false));

            verify(commentService, never()).createComment(any(), any());
        }

        @Test
        @DisplayName("Create comment failed: Missing content")
        void createComment_Fail_MissingContent() throws Exception {
            CreateCommentRequest request = new CreateCommentRequest(POST_ID, null, "", null);

            mockMvc.perform(post("/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(commentService, never()).createComment(any(), any());
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
        void getRepliesByComment_Success() throws Exception {
            CommentResponse reply = buildCommentResponse(101L, COMMENT_ID, "This is a reply");
            Page<CommentResponse> page = new PageImpl<>(List.of(reply), PageRequest.of(0, 20), 1);
            PageResponse<CommentResponse> pageResponse = PageResponse.of(page);

            when(commentService.getRepliesByComment(eq(COMMENT_ID), any(Pageable.class), eq(USER_ID)))
                    .thenReturn(pageResponse);

            mockMvc.perform(get("/comments/{commentId}/replies", COMMENT_ID)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(101));

            verify(commentService).getRepliesByComment(eq(COMMENT_ID), any(Pageable.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Get replies failed: Invalid negative commentId")
        void getRepliesByComment_Fail_NegativeId() throws Exception {
            mockMvc.perform(get("/comments/{commentId}/replies", -5L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(commentService, never()).getRepliesByComment(any(), any(), any());
        }
    }

    // ========================================================================
    // updateComment
    // ========================================================================
    @Nested
    @DisplayName("updateComment")
    class UpdateCommentTests {

        @Test
        @DisplayName("Update comment successfully")
        void updateComment_Success() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", null);
            CommentResponse response = buildCommentResponse(COMMENT_ID, null, "Updated content");

            when(commentService.updateComment(eq(COMMENT_ID), any(UpdateCommentRequest.class), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/comments/{commentId}", COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").value("Updated content"));

            verify(commentService).updateComment(eq(COMMENT_ID), any(UpdateCommentRequest.class), eq(USER_ID));
        }

        @Test
        @DisplayName("Update comment failed: Access denied")
        void updateComment_Fail_AccessDenied() throws Exception {
            UpdateCommentRequest request = new UpdateCommentRequest("Updated content", null);
            when(commentService.updateComment(eq(COMMENT_ID), any(UpdateCommentRequest.class), eq(USER_ID)))
                    .thenThrow(new AppException(AppError.COMMENT_ACCESS_DENIED));

            mockMvc.perform(put("/comments/{commentId}", COMMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========================================================================
    // deleteComment
    // ========================================================================
    @Nested
    @DisplayName("deleteComment")
    class DeleteCommentTests {

        @Test
        @DisplayName("Delete comment successfully")
        void deleteComment_Success() throws Exception {
            doNothing().when(commentService).deleteComment(COMMENT_ID, USER_ID);

            mockMvc.perform(delete("/comments/{commentId}", COMMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).deleteComment(COMMENT_ID, USER_ID);
        }

        @Test
        @DisplayName("Delete comment failed: Comment not found")
        void deleteComment_Fail_NotFound() throws Exception {
            doThrow(new AppException(AppError.COMMENT_NOT_FOUND))
                    .when(commentService).deleteComment(COMMENT_ID, USER_ID);

            mockMvc.perform(delete("/comments/{commentId}", COMMENT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ========================================================================
    // likeComment / unlikeComment
    // ========================================================================
    @Nested
    @DisplayName("likeComment and unlikeComment")
    class LikeAndUnlikeCommentTests {

        @Test
        @DisplayName("Like comment successfully")
        void likeComment_Success() throws Exception {
            doNothing().when(commentService).likeComment(COMMENT_ID, USER_ID);

            mockMvc.perform(post("/comments/{commentId}/like", COMMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).likeComment(COMMENT_ID, USER_ID);
        }

        @Test
        @DisplayName("Like comment failed: Already liked")
        void likeComment_Fail_AlreadyLiked() throws Exception {
            doThrow(new AppException(AppError.COMMENT_ALREADY_LIKED))
                    .when(commentService).likeComment(COMMENT_ID, USER_ID);

            mockMvc.perform(post("/comments/{commentId}/like", COMMENT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Unlike comment successfully")
        void unlikeComment_Success() throws Exception {
            doNothing().when(commentService).unlikeComment(COMMENT_ID, USER_ID);

            mockMvc.perform(delete("/comments/{commentId}/like", COMMENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(commentService).unlikeComment(COMMENT_ID, USER_ID);
        }

        @Test
        @DisplayName("Unlike comment failed: Not liked yet")
        void unlikeComment_Fail_NotLiked() throws Exception {
            doThrow(new AppException(AppError.COMMENT_NOT_LIKED))
                    .when(commentService).unlikeComment(COMMENT_ID, USER_ID);

            mockMvc.perform(delete("/comments/{commentId}/like", COMMENT_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
