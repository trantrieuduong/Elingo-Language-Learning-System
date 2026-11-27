package com.elingo.admin.community.group.service;

import com.elingo.admin.community.group.service.impl.AdminCommunityGroupServiceImpl;
import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import com.elingo.community.service.PostGroupService;
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
@DisplayName("AdminCommunityGroupService")
class AdminCommunityGroupServiceTest {

    @Mock
    private PostGroupService postGroupService;

    @InjectMocks
    private AdminCommunityGroupServiceImpl adminCommunityGroupService;

    private static final Long ADMIN_ID = 1L;
    private static final Long GROUP_ID = 5L;

    private static final AdminPostGroupResponse SAMPLE_RESPONSE = new AdminPostGroupResponse(
            GROUP_ID, "English Grammar", "Grammar rules", 10L,
            LocalDateTime.now(), LocalDateTime.now()
    );

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("should delegate to postGroupService when sort field is allowed")
        void shouldDelegate_whenSortAllowed() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by("groupName").ascending());
            PageResponse<AdminPostGroupResponse> expected = new PageResponse<>(
                    List.of(SAMPLE_RESPONSE), 1, 20, 1L, 1, true);
            when(postGroupService.searchForAdmin("Grammar", pageable)).thenReturn(expected);

            PageResponse<AdminPostGroupResponse> result = adminCommunityGroupService.search("Grammar", pageable);

            assertThat(result).isEqualTo(expected);
            verify(postGroupService).searchForAdmin("Grammar", pageable);
        }

        @ParameterizedTest
        @ValueSource(strings = {"description", "postCount", "unknown"})
        @DisplayName("should throw INVALID_SORT when sort field is not in whitelist")
        void shouldThrowInvalidSort_whenNotAllowed(String sortField) {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(sortField));

            assertThatThrownBy(() -> adminCommunityGroupService.search(null, pageable))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.INVALID_SORT));
            verify(postGroupService, never()).searchForAdmin(any(), any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should delegate getById to postGroupService")
        void shouldReturnDetail() {
            when(postGroupService.getByIdForAdmin(GROUP_ID)).thenReturn(SAMPLE_RESPONSE);

            AdminPostGroupResponse result = adminCommunityGroupService.getById(GROUP_ID);

            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(postGroupService).getByIdForAdmin(GROUP_ID);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should delegate create to postGroupService")
        void shouldCreateGroup() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("English Grammar", "Grammar rules");
            when(postGroupService.createForAdmin(request)).thenReturn(SAMPLE_RESPONSE);

            AdminPostGroupResponse result = adminCommunityGroupService.create(ADMIN_ID, request);

            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(postGroupService).createForAdmin(request);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should delegate update to postGroupService")
        void shouldUpdateGroup() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("Updated", null);
            when(postGroupService.updateForAdmin(GROUP_ID, request)).thenReturn(SAMPLE_RESPONSE);

            AdminPostGroupResponse result = adminCommunityGroupService.update(ADMIN_ID, GROUP_ID, request);

            assertThat(result).isEqualTo(SAMPLE_RESPONSE);
            verify(postGroupService).updateForAdmin(GROUP_ID, request);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delegate delete to postGroupService")
        void shouldDeleteGroup() {
            adminCommunityGroupService.delete(ADMIN_ID, GROUP_ID);

            verify(postGroupService).deleteForAdmin(GROUP_ID);
        }
    }
}
