package com.elingo.community.service;

import com.elingo.common.dto.PageResponse;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.community.dto.projection.PostGroupCountProjection;
import com.elingo.community.dto.request.AdminPostGroupRequest;
import com.elingo.community.dto.response.AdminPostGroupResponse;
import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.entity.PostGroup;
import com.elingo.community.mapper.PostGroupMapper;
import com.elingo.community.repository.PostGroupRepository;
import com.elingo.community.repository.PostRepository;
import com.elingo.community.service.impl.PostGroupServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostGroupServiceTest {

    @Mock
    private PostGroupRepository repository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostGroupMapper mapper;

    @InjectMocks
    private PostGroupServiceImpl postGroupService;

    @Nested
    @DisplayName("getPostGroups")
    class GetPostGroupsTests {

        @Test
        @DisplayName("Get post groups successfully when data exists")
        void getPostGroups_Success() {
            PostGroup g1 = PostGroup.builder().id(1L).groupName("Group 1").description("Desc 1").build();
            PostGroup g2 = PostGroup.builder().id(2L).groupName("Group 2").description("Desc 2").build();
            PostGroupResponse r1 = new PostGroupResponse(1L, "Group 1", "Desc 1");
            PostGroupResponse r2 = new PostGroupResponse(2L, "Group 2", "Desc 2");

            when(repository.findAll()).thenReturn(List.of(g1, g2));
            when(mapper.toPostGroupResponse(g1)).thenReturn(r1);
            when(mapper.toPostGroupResponse(g2)).thenReturn(r2);

            List<PostGroupResponse> results = postGroupService.getPostGroups();

            assertThat(results).hasSize(2).containsExactly(r1, r2);
            verify(repository).findAll();
            verify(mapper).toPostGroupResponse(g1);
            verify(mapper).toPostGroupResponse(g2);
        }

        @Test
        @DisplayName("Get post groups successfully when empty")
        void getPostGroups_Success_Empty() {
            when(repository.findAll()).thenReturn(List.of());

            List<PostGroupResponse> results = postGroupService.getPostGroups();

            assertThat(results).isEmpty();
            verify(repository).findAll();
        }
    }

    private AdminPostGroupResponse buildAdminResponse(Long id, String name, String desc, long postCount) {
        return new AdminPostGroupResponse(id, name, desc, postCount, LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("searchForAdmin")
    class SearchForAdmin {

        @Test
        @DisplayName("should return empty page without querying post counts when no groups match")
        void shouldReturnEmptyPage_whenNoGroupsMatch() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<PostGroup> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(repository.searchForAdmin(null, pageable)).thenReturn(emptyPage);

            PageResponse<AdminPostGroupResponse> result = postGroupService.searchForAdmin("   ", pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(postRepository, never()).countPostsByGroupIds(any());
        }

        @Test
        @DisplayName("should trim keyword and batch-load post counts")
        void shouldReturnPagedGroupsWithPostCounts() {
            Pageable pageable = PageRequest.of(0, 20);
            PostGroup g1 = PostGroup.builder().id(1L).groupName("Grammar").description("Desc").build();
            PostGroup g2 = PostGroup.builder().id(2L).groupName("Speaking").description(null).build();
            Page<PostGroup> page = new PageImpl<>(List.of(g1, g2), pageable, 2);

            PostGroupCountProjection proj1 = mock(PostGroupCountProjection.class);
            when(proj1.getGroupId()).thenReturn(1L);
            when(proj1.getPostCount()).thenReturn(12L);

            AdminPostGroupResponse r1 = buildAdminResponse(1L, "Grammar", "Desc", 12L);
            AdminPostGroupResponse r2 = buildAdminResponse(2L, "Speaking", null, 0L);

            when(repository.searchForAdmin("Grammar", pageable)).thenReturn(page);
            when(postRepository.countPostsByGroupIds(List.of(1L, 2L))).thenReturn(List.of(proj1));
            when(mapper.toAdminResponse(g1, 12L)).thenReturn(r1);
            when(mapper.toAdminResponse(g2, 0L)).thenReturn(r2);

            PageResponse<AdminPostGroupResponse> result = postGroupService.searchForAdmin("  Grammar  ", pageable);

            assertThat(result.content()).containsExactly(r1, r2);
            assertThat(result.totalElements()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("getByIdForAdmin")
    class GetByIdForAdmin {

        @Test
        @DisplayName("should return group detail with post count when id exists")
        void shouldReturnDetail_whenExists() {
            PostGroup group = PostGroup.builder().id(1L).groupName("Grammar").description("Desc").build();
            AdminPostGroupResponse expected = buildAdminResponse(1L, "Grammar", "Desc", 5L);

            when(repository.findById(1L)).thenReturn(Optional.of(group));
            when(postRepository.countByGroupId(1L)).thenReturn(5L);
            when(mapper.toAdminResponse(group, 5L)).thenReturn(expected);

            AdminPostGroupResponse result = postGroupService.getByIdForAdmin(1L);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw POST_GROUP_NOT_FOUND when id does not exist")
        void shouldThrowNotFound_whenMissing() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postGroupService.getByIdForAdmin(99L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createForAdmin")
    class CreateForAdmin {

        @Test
        @DisplayName("should create post group with trimmed fields when name is unique")
        void shouldCreateGroup_whenUnique() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("  Grammar  ", "  Rules  ");
            AdminPostGroupRequest normalized = new AdminPostGroupRequest("Grammar", "Rules");
            PostGroup entity = PostGroup.builder().groupName("Grammar").description("Rules").build();
            PostGroup saved = PostGroup.builder().id(1L).groupName("Grammar").description("Rules").build();
            AdminPostGroupResponse expected = buildAdminResponse(1L, "Grammar", "Rules", 0L);

            when(repository.existsByGroupNameIgnoreCase("Grammar")).thenReturn(false);
            when(mapper.toEntity(normalized)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(saved);
            when(mapper.toAdminResponse(saved, 0L)).thenReturn(expected);

            AdminPostGroupResponse result = postGroupService.createForAdmin(request);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should handle null description when creating post group")
        void shouldCreateGroup_whenDescriptionNull() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("Grammar", null);
            PostGroup entity = PostGroup.builder().groupName("Grammar").description(null).build();
            PostGroup saved = PostGroup.builder().id(1L).groupName("Grammar").description(null).build();
            AdminPostGroupResponse expected = buildAdminResponse(1L, "Grammar", null, 0L);

            when(repository.existsByGroupNameIgnoreCase("Grammar")).thenReturn(false);
            when(mapper.toEntity(request)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(saved);
            when(mapper.toAdminResponse(saved, 0L)).thenReturn(expected);

            AdminPostGroupResponse result = postGroupService.createForAdmin(request);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw POST_GROUP_ALREADY_EXISTS when group name duplicates")
        void shouldThrowDuplicate_whenNameExists() {
            AdminPostGroupRequest request = new AdminPostGroupRequest("Grammar", "Desc");
            when(repository.existsByGroupNameIgnoreCase("Grammar")).thenReturn(true);

            assertThatThrownBy(() -> postGroupService.createForAdmin(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_ALREADY_EXISTS));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateForAdmin")
    class UpdateForAdmin {

        @Test
        @DisplayName("should update post group and overwrite fields including null description")
        void shouldUpdateGroup_whenValid() {
            PostGroup existing = PostGroup.builder().id(1L).groupName("Old").description("Old desc").build();
            AdminPostGroupRequest request = new AdminPostGroupRequest("  New  ", null);
            AdminPostGroupRequest normalized = new AdminPostGroupRequest("New", null);
            AdminPostGroupResponse expected = buildAdminResponse(1L, "New", null, 3L);

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByGroupNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(false);
            when(repository.save(existing)).thenReturn(existing);
            when(postRepository.countByGroupId(1L)).thenReturn(3L);
            when(mapper.toAdminResponse(existing, 3L)).thenReturn(expected);

            AdminPostGroupResponse result = postGroupService.updateForAdmin(1L, request);

            assertThat(result).isEqualTo(expected);
            verify(mapper).updateEntity(existing, normalized);
        }

        @Test
        @DisplayName("should throw POST_GROUP_NOT_FOUND when updating non-existent group")
        void shouldThrowNotFound_whenMissing() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postGroupService.updateForAdmin(99L, new AdminPostGroupRequest("New", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_NOT_FOUND));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("should throw POST_GROUP_ALREADY_EXISTS when new name conflicts with another group")
        void shouldThrowDuplicate_whenNameConflicts() {
            PostGroup existing = PostGroup.builder().id(1L).groupName("Old").build();
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.existsByGroupNameIgnoreCaseAndIdNot("Grammar", 1L)).thenReturn(true);

            assertThatThrownBy(() -> postGroupService.updateForAdmin(1L, new AdminPostGroupRequest("Grammar", null)))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_ALREADY_EXISTS));
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteForAdmin")
    class DeleteForAdmin {

        @Test
        @DisplayName("should delete post group when no posts belong to it")
        void shouldDeleteGroup_whenEmpty() {
            PostGroup existing = PostGroup.builder().id(1L).groupName("Grammar").build();
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.existsByGroupId(1L)).thenReturn(false);

            postGroupService.deleteForAdmin(1L);

            verify(repository).delete(existing);
        }

        @Test
        @DisplayName("should throw POST_GROUP_NOT_FOUND when deleting missing group")
        void shouldThrowNotFound_whenMissing() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postGroupService.deleteForAdmin(99L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_NOT_FOUND));
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw POST_GROUP_IN_USE when posts still exist in group")
        void shouldThrowInUse_whenPostsExist() {
            PostGroup existing = PostGroup.builder().id(1L).groupName("Grammar").build();
            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(postRepository.existsByGroupId(1L)).thenReturn(true);

            assertThatThrownBy(() -> postGroupService.deleteForAdmin(1L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getAppError())
                            .isEqualTo(AppError.POST_GROUP_IN_USE));
            verify(repository, never()).delete(any());
        }
    }
}

