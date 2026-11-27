package com.elingo.admin.community.moderation.controller;

import com.elingo.admin.community.moderation.dto.request.ApprovePostRequest;
import com.elingo.admin.community.moderation.dto.request.RejectPostRequest;
import com.elingo.admin.community.moderation.service.AdminCommunityModerationService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.request.AdminModerationLogFilterRequest;
import com.elingo.community.dto.request.AdminPostModerationSearchRequest;
import com.elingo.community.dto.response.AdminPostModerationDetailResponse;
import com.elingo.community.dto.response.AdminPostModerationResponse;
import com.elingo.community.dto.response.AuthorResponse;
import com.elingo.community.dto.response.PostModerationLogResponse;
import com.elingo.community.entity.ModerationAction;
import com.elingo.community.entity.ModerationActorType;
import com.elingo.community.entity.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminCommunityModerationController")
class AdminCommunityModerationControllerTest {

    @Mock
    private AdminCommunityModerationService adminCommunityModerationService;

    @InjectMocks
    private AdminCommunityModerationController adminCommunityModerationController;

    private static final Long ADMIN_ID = 1L;
    private static final Long POST_ID = 105L;

    private static final AdminPostModerationResponse SAMPLE_POST_RESPONSE = new AdminPostModerationResponse(
            POST_ID, 1L, "English Grammar",
            new AuthorResponse(15L, "john_doe", "John Doe", null),
            "Tips for IELTS", "Post content", PostStatus.NEEDS_REVIEW,
            0, 0, List.of(), null, LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("searchPosts")
    class SearchPosts {

        @Test
        @DisplayName("should return ApiResponse with PageResponse of posts")
        void shouldReturnPageResponse_whenValidRequest() {
            // given
            AdminPostModerationSearchRequest filter = new AdminPostModerationSearchRequest(
                    PostStatus.NEEDS_REVIEW, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<AdminPostModerationResponse> page = new PageResponse<>(
                    List.of(SAMPLE_POST_RESPONSE), 1, 20, 1L, 1, true);

            when(adminCommunityModerationService.searchPosts(filter, pageable)).thenReturn(page);

            // when
            ApiResponse<PageResponse<AdminPostModerationResponse>> response =
                    adminCommunityModerationController.searchPosts(filter, pageable);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            verify(adminCommunityModerationService).searchPosts(filter, pageable);
        }
    }

    @Nested
    @DisplayName("getPostDetail")
    class GetPostDetail {

        @Test
        @DisplayName("should return ApiResponse with post moderation detail")
        void shouldReturnDetail_whenPostExists() {
            // given
            AdminPostModerationDetailResponse detail = new AdminPostModerationDetailResponse(
                    POST_ID, 1L, "English Grammar",
                    new AuthorResponse(15L, "john_doe", "John Doe", null),
                    "Tips for IELTS", "Post content", PostStatus.NEEDS_REVIEW,
                    0, 0, List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
            );
            when(adminCommunityModerationService.getPostDetail(POST_ID)).thenReturn(detail);

            // when
            ApiResponse<AdminPostModerationDetailResponse> response =
                    adminCommunityModerationController.getPostDetail(POST_ID);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(detail);
            verify(adminCommunityModerationService).getPostDetail(POST_ID);
        }

        @Test
        @DisplayName("should propagate AppException when post is not found")
        void shouldPropagateException_whenNotFound() {
            // given
            when(adminCommunityModerationService.getPostDetail(POST_ID))
                    .thenThrow(new AppException(AppError.POST_NOT_FOUND));

            // when / then
            assertThatThrownBy(() -> adminCommunityModerationController.getPostDetail(POST_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("searchModerationLogs")
    class SearchModerationLogs {

        @Test
        @DisplayName("should return ApiResponse with PageResponse of logs")
        void shouldReturnPageResponse_whenValidRequest() {
            // given
            AdminModerationLogFilterRequest filter = new AdminModerationLogFilterRequest(
                    POST_ID, ModerationActorType.AI, ModerationAction.FLAG_FOR_REVIEW, null, null, null);
            Pageable pageable = PageRequest.of(0, 20);
            PostModerationLogResponse logItem = new PostModerationLogResponse(
                    201L, POST_ID, ModerationActorType.AI, null, null,
                    ModerationAction.FLAG_FOR_REVIEW, "SPAM", null, "Flagged", LocalDateTime.now());
            PageResponse<PostModerationLogResponse> page = new PageResponse<>(
                    List.of(logItem), 1, 20, 1L, 1, true);

            when(adminCommunityModerationService.searchModerationLogs(filter, pageable)).thenReturn(page);

            // when
            ApiResponse<PageResponse<PostModerationLogResponse>> response =
                    adminCommunityModerationController.searchModerationLogs(filter, pageable);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            verify(adminCommunityModerationService).searchModerationLogs(filter, pageable);
        }
    }

    @Nested
    @DisplayName("approvePost")
    class ApprovePost {

        @Test
        @DisplayName("should approve post and return ApiResponse")
        void shouldApprovePost_whenValid() {
            // given
            ApprovePostRequest request = new ApprovePostRequest("Safe");
            when(adminCommunityModerationService.approvePost(ADMIN_ID, POST_ID, request))
                    .thenReturn(SAMPLE_POST_RESPONSE);

            // when
            ApiResponse<AdminPostModerationResponse> response =
                    adminCommunityModerationController.approvePost(ADMIN_ID, POST_ID, request);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_POST_RESPONSE);
            verify(adminCommunityModerationService).approvePost(ADMIN_ID, POST_ID, request);
        }
    }

    @Nested
    @DisplayName("rejectPost")
    class RejectPost {

        @Test
        @DisplayName("should reject post and return ApiResponse")
        void shouldRejectPost_whenValid() {
            // given
            RejectPostRequest request = new RejectPostRequest("Spam");
            when(adminCommunityModerationService.rejectPost(ADMIN_ID, POST_ID, request))
                    .thenReturn(SAMPLE_POST_RESPONSE);

            // when
            ApiResponse<AdminPostModerationResponse> response =
                    adminCommunityModerationController.rejectPost(ADMIN_ID, POST_ID, request);

            // then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_POST_RESPONSE);
            verify(adminCommunityModerationService).rejectPost(ADMIN_ID, POST_ID, request);
        }
    }
}
