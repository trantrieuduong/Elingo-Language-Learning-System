package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.entity.Tag;
import com.elingo.vocabulary.mapper.TagMapper;
import com.elingo.vocabulary.repository.TagRepository;
import com.elingo.vocabulary.service.impl.TagServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@org.junit.jupiter.api.Tag("unit")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagServiceImpl tagService;

    @Nested
    @DisplayName("getAllTags")
    class GetAllTagsTests {

        @Test
        @DisplayName("Get all tags successfully: maps every entity")
        void getAllTags_Success() {
            Tag travel = Tag.builder().id(1L).label("Travel").build();
            Tag food = Tag.builder().id(2L).label("Food").build();
            TagResponse travelResponse = new TagResponse(1L, "Travel");
            TagResponse foodResponse = new TagResponse(2L, "Food");

            when(tagRepository.findAll()).thenReturn(List.of(travel, food));
            when(tagMapper.toTagResponse(travel)).thenReturn(travelResponse);
            when(tagMapper.toTagResponse(food)).thenReturn(foodResponse);

            List<TagResponse> result = tagService.getAllTags();

            assertThat(result).containsExactly(travelResponse, foodResponse);
            verify(tagRepository).findAll();
        }

        @Test
        @DisplayName("Get all tags successfully: returns empty list when there is no data")
        void getAllTags_Success_Empty() {
            when(tagRepository.findAll()).thenReturn(List.of());

            assertThat(tagService.getAllTags()).isEmpty();
        }
    }
}
