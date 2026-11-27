package com.elingo.admin.community.moderation.service;

import com.elingo.admin.community.moderation.dto.request.ApprovePostRequest;
import com.elingo.admin.community.moderation.dto.request.RejectPostRequest;
import com.elingo.admin.community.moderation.service.impl.AdminCommunityModerationServiceImpl;
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
import com.elingo.community.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AdminCommunityModerationService")
class AdminCommunityModerationServiceTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private AdminCommunityModerationServiceImpl adminCommunityModerationService;

    private static final Long ADMIN_ID = 1L;
    private static final Long POST_ID = 105L;

    private static final AdminPostModerationResponse SAMPLE_POST_RESPONSE = new AdminPostModerationResponse(
            POST_ID,
            1L,
            "English Grammar",
            new AuthorResponse(15L, "john_doe", "John Doe", null),
            "Tips for IELTS",
            "Post content",
            PostStatus.NEEDS_REVIEW,
            0,
            0,
            List.of(),
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
    );

    @Nested
    @DisplayName("searchPosts")
    class SearchPosts {

        @Test
        @DisplayName("should delegate to postService when sort field is allowed")
        void shouldDelegateToPostService_whenSortIsAllowed() {
            // given
            AdminPostModerationSearchRequest filter = new AdminPostModerationSearchRequest(
                    PostStatus.NEEDS_REVIEW, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            PageResponse<AdminPostModerationResponse> expected = new PageResponse<>(
                    List.of(SAMPLE_POST_RESPONSE), 1, 20, 1L, 1, true);

            when(postService.searchPostsForModeration(filter, pageable)).thenReturn(expected);

            // when
            PageResponse<AdminPostModerationResponse> result = adminCommunityModerationService.searchPosts(filter, pageable);

            // then
            assertThat(result).isEqualTo(expected);
            verify(postService).searchPostsForModeration(filter, pageable);
        }

        @ParameterizedTest
        @ValueSource(strings = {"title", "status", "likeCount", "unknown"})
        @DisplayName("should throw INVALID_SORT when post sort field is not in whitelist")
        void shouldThrowInvalidSort_whenPostSortNotAllowed(String sortField) {
            // given
            AdminPostModerationSearchRequest filter = new AdminPostModerationSearchRequest(
                    null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by(sortField));

            // when / then
            assertThatThrownBy(() -> adminCommunityModerationService.searchPosts(filter, pageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_SORT));
            verify(postService, never()).searchPostsForModeration(any(), any());
        }
    }

    @Nested
    @DisplayName("getPostDetail")
    class GetPostDetail {

        @Test
        @DisplayName("should return post moderation detail from postService")
        void shouldReturnPostDetail_whenPostExists() {
            // given
            AdminPostModerationDetailResponse expected = new AdminPostModerationDetailResponse(
                    POST_ID, 1L, "English Grammar",
                    new AuthorResponse(15L, "john_doe", "John Doe", null),
                    "Tips for IELTS", "Post content", PostStatus.NEEDS_REVIEW,
                    0, 0, List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
            );
            when(postService.getPostModerationDetail(POST_ID)).thenReturn(expected);

            // when
            AdminPostModerationDetailResponse result = adminCommunityModerationService.getPostDetail(POST_ID);

            // then
            assertThat(result).isEqualTo(expected);
            verify(postService).getPostModerationDetail(POST_ID);
        }
    }

    @Nested
    @DisplayName("searchModerationLogs")
    class SearchModerationLogs {

        @Test
        @DisplayName("should delegate to postService when log sort field is allowed")
        void shouldDelegateToPostService_whenLogSortIsAllowed() {
            // given
            AdminModerationLogFilterRequest filter = new AdminModerationLogFilterRequest(
                    POST_ID, ModerationActorType.AI, ModerationAction.FLAG_FOR_REVIEW, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by("id").descending());
            PostModerationLogResponse logItem = new PostModerationLogResponse(
                    201L, POST_ID, ModerationActorType.AI, null, null,
                    ModerationAction.FLAG_FOR_REVIEW, "SPAM", null, "Flagged", LocalDateTime.now());
            PageResponse<PostModerationLogResponse> expected = new PageResponse<>(
                    List.of(logItem), 1, 20, 1L, 1, true);

            when(postService.searchModerationLogs(filter, pageable)).thenReturn(expected);

            // when
            PageResponse<PostModerationLogResponse> result =
                    adminCommunityModerationService.searchModerationLogs(filter, pageable);

            // then
            assertThat(result).isEqualTo(expected);
            verify(postService).searchModerationLogs(filter, pageable);
        }

        @ParameterizedTest
        @ValueSource(strings = {"action", "aiScore", "actorType", "unknown"})
        @DisplayName("should throw INVALID_SORT when log sort field is not in whitelist")
        void shouldThrowInvalidSort_whenLogSortNotAllowed(String sortField) {
            // given
            AdminModerationLogFilterRequest filter = new AdminModerationLogFilterRequest(
                    null, null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 20, Sort.by(sortField));

            // when / then
            assertThatThrownBy(() -> adminCommunityModerationService.searchModerationLogs(filter, pageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_SORT));
            verify(postService, never()).searchModerationLogs(any(), any());
        }
    }

    @Nested
    @DisplayName("approvePost")
    class ApprovePost {

        @Test
        @DisplayName("should pass note when approve request is provided")
        void shouldPassNote_whenRequestProvided() {
            // given
            ApprovePostRequest request = new ApprovePostRequest("Verified safe");
            when(postService.approvePostByAdmin(ADMIN_ID, POST_ID, "Verified safe"))
                    .thenReturn(SAMPLE_POST_RESPONSE);

            // when
            AdminPostModerationResponse result =
                    adminCommunityModerationService.approvePost(ADMIN_ID, POST_ID, request);

            // then
            assertThat(result).isEqualTo(SAMPLE_POST_RESPONSE);
            verify(postService).approvePostByAdmin(ADMIN_ID, POST_ID, "Verified safe");
        }

        @Test
        @DisplayName("should pass null note when approve request is null")
        void shouldPassNullNote_whenRequestIsNull() {
            // given
            when(postService.approvePostByAdmin(ADMIN_ID, POST_ID, null))
                    .thenReturn(SAMPLE_POST_RESPONSE);

            // when
            AdminPostModerationResponse result =
                    adminCommunityModerationService.approvePost(ADMIN_ID, POST_ID, null);

            // then
            assertThat(result).isEqualTo(SAMPLE_POST_RESPONSE);
            verify(postService).approvePostByAdmin(ADMIN_ID, POST_ID, null);
        }
    }

    @Nested
    @DisplayName("rejectPost")
    class RejectPost {

        @Test
        @DisplayName("should delegate rejection with reason to postService")
        void shouldDelegateRejection_whenValidRequest() {
            // given
            RejectPostRequest request = new RejectPostRequest("Promotional spam");
            when(postService.rejectPostByAdmin(ADMIN_ID, POST_ID, "Promotional spam"))
                    .thenReturn(SAMPLE_POST_RESPONSE);

            // when
            AdminPostModerationResponse result =
                    adminCommunityModerationService.rejectPost(ADMIN_ID, POST_ID, request);

            // then
            assertThat(result).isEqualTo(SAMPLE_POST_RESPONSE);
            verify(postService).rejectPostByAdmin(ADMIN_ID, POST_ID, "Promotional spam");
        }
    }
}
