package com.elingo.admin.community.group.controller;

import com.elingo.admin.community.group.service.AdminCommunityGroupService;
import com.elingo.common.dto.ApiResponse;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
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
@DisplayName("AdminCommunityGroupController")
class AdminCommunityGroupControllerTest {

    @Mock
    private AdminCommunityGroupService adminCommunityGroupService;

    @InjectMocks
    private AdminCommunityGroupController adminCommunityGroupController;

    private static final Long ADMIN_ID = 1L;
    private static final Long GROUP_ID = 5L;

    private static final AdminPostGroupResponse SAMPLE_RESPONSE = new AdminPostGroupResponse(
            GROUP_ID, "English Grammar", "Grammar rules", 10L,
            LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("searchGroups")
    class SearchGroups {

        @Test
        @DisplayName("should return ApiResponse with PageResponse")
        void shouldReturnPageResponse() {
            Pageable pageable = PageRequest.of(0, 20);
            PageResponse<AdminPostGroupResponse> page = new PageResponse<>(
                    List.of(SAMPLE_RESPONSE), 1, 20, 1L, 1, true);
            when(adminCommunityGroupService.search("Grammar", pageable)).thenReturn(page);

            ApiResponse<PageResponse<AdminPostGroupResponse>> response =
                    adminCommunityGroupController.searchGroups("Grammar", pageable);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(page);
            verify(adminCommunityGroupService).search("Grammar", pageable);
        }
    }

    @Nested
    @DisplayName("getGroupById")
    class GetGroupById {

        @Test
        @DisplayName("should return ApiResponse with group detail")
        void shouldReturnDetail() {
            when(adminCommunityGroupService.getById(GROUP_ID)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminPostGroupResponse> response = adminCommunityGroupController.getGroupById(GROUP_ID);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_RESPONSE);
        }

        @Test
        @DisplayName("should propagate AppException when group not found")
        void shouldPropagateNotFound() {
            when(adminCommunityGroupService.getById(GROUP_ID))
                    .thenThrow(new AppException(AppError.POST_GROUP_NOT_FOUND));

            assertThatThrownBy(() -> adminCommunityGroupController.getGroupById(GROUP_ID))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createGroup")
    class CreateGroup {

        @Test
        @DisplayName("should create group and return ApiResponse")
        void shouldCreateGroup() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("English Grammar", "Grammar rules");
            when(adminCommunityGroupService.create(ADMIN_ID, request)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminPostGroupResponse> response =
                    adminCommunityGroupController.createGroup(ADMIN_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_RESPONSE);
            verify(adminCommunityGroupService).create(ADMIN_ID, request);
        }
    }

    @Nested
    @DisplayName("updateGroup")
    class UpdateGroup {

        @Test
        @DisplayName("should update group and return ApiResponse")
        void shouldUpdateGroup() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("Updated", null);
            when(adminCommunityGroupService.update(ADMIN_ID, GROUP_ID, request)).thenReturn(SAMPLE_RESPONSE);

            ApiResponse<AdminPostGroupResponse> response =
                    adminCommunityGroupController.updateGroup(ADMIN_ID, GROUP_ID, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(SAMPLE_RESPONSE);
            verify(adminCommunityGroupService).update(ADMIN_ID, GROUP_ID, request);
        }
    }

    @Nested
    @DisplayName("deleteGroup")
    class DeleteGroup {

        @Test
        @DisplayName("should delete group and return success ApiResponse")
        void shouldDeleteGroup() {
            ApiResponse<Void> response = adminCommunityGroupController.deleteGroup(ADMIN_ID, GROUP_ID);

            assertThat(response.isSuccess()).isTrue();
            verify(adminCommunityGroupService).delete(ADMIN_ID, GROUP_ID);
        }
    }
}
