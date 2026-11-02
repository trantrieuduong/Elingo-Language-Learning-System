package com.elingo.community.service;

import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.entity.PostGroup;
import com.elingo.community.mapper.PostGroupMapper;
import com.elingo.community.repository.PostGroupRepository;
import com.elingo.community.service.impl.PostGroupServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostGroupServiceTest {

    @Mock
    private PostGroupRepository repository;

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
}

