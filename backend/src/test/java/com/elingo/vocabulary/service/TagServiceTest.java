package com.elingo.vocabulary.service;

import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Nested
    @DisplayName("getTagById")
    class GetTagByIdTests {

        @Test
        @DisplayName("Get tag by ID successfully when tag exists")
        void getTagById_Success() {
            Tag tag = Tag.builder().id(1L).label("Grammar").build();
            TagResponse expected = new TagResponse(1L, "Grammar");

            when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
            when(tagMapper.toTagResponse(tag)).thenReturn(expected);

            TagResponse actual = tagService.getTagById(1L);

            assertThat(actual).isEqualTo(expected);
            verify(tagRepository).findById(1L);
        }

        @Test
        @DisplayName("Throw TAG_NOT_FOUND when tag does not exist")
        void getTagById_NotFound() {
            when(tagRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.getTagById(99L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.TAG_NOT_FOUND);

            verify(tagRepository).findById(99L);
        }
    }

    @Nested
    @DisplayName("createTag")
    class CreateTagTests {

        @Test
        @DisplayName("Create tag successfully when label is unique")
        void createTag_Success() {
            TagRequest request = new TagRequest("Technology");
            Tag savedTag = Tag.builder().id(10L).label("Technology").build();
            TagResponse expected = new TagResponse(10L, "Technology");

            when(tagRepository.existsByLabelIgnoreCase("Technology")).thenReturn(false);
            when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);
            when(tagMapper.toTagResponse(savedTag)).thenReturn(expected);

            TagResponse actual = tagService.createTag(request);

            assertThat(actual).isEqualTo(expected);
            verify(tagRepository).existsByLabelIgnoreCase("Technology");
            verify(tagRepository).save(any(Tag.class));
        }

        @Test
        @DisplayName("Throw TAG_LABEL_EXISTED when label already exists")
        void createTag_DuplicateLabel() {
            TagRequest request = new TagRequest("Technology");
            when(tagRepository.existsByLabelIgnoreCase("Technology")).thenReturn(true);

            assertThatThrownBy(() -> tagService.createTag(request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.TAG_LABEL_EXISTED);

            verify(tagRepository).existsByLabelIgnoreCase("Technology");
            verify(tagRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateTag")
    class UpdateTagTests {

        @Test
        @DisplayName("Update tag successfully with valid new label")
        void updateTag_Success() {
            Tag existing = Tag.builder().id(5L).label("Old Label").build();
            Tag updated = Tag.builder().id(5L).label("New Label").build();
            TagRequest request = new TagRequest("New Label");
            TagResponse expected = new TagResponse(5L, "New Label");

            when(tagRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(tagRepository.existsByLabelIgnoreCaseAndIdNot("New Label", 5L)).thenReturn(false);
            when(tagRepository.save(existing)).thenReturn(updated);
            when(tagMapper.toTagResponse(updated)).thenReturn(expected);

            TagResponse actual = tagService.updateTag(5L, request);

            assertThat(actual).isEqualTo(expected);
            verify(tagRepository).findById(5L);
            verify(tagRepository).existsByLabelIgnoreCaseAndIdNot("New Label", 5L);
            verify(tagRepository).save(existing);
        }

        @Test
        @DisplayName("Throw TAG_NOT_FOUND when updating non-existent tag")
        void updateTag_NotFound() {
            TagRequest request = new TagRequest("New Label");
            when(tagRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.updateTag(99L, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.TAG_NOT_FOUND);

            verify(tagRepository).findById(99L);
            verify(tagRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throw TAG_LABEL_EXISTED when new label conflicts with another tag")
        void updateTag_DuplicateLabel() {
            Tag existing = Tag.builder().id(5L).label("Old Label").build();
            TagRequest request = new TagRequest("Existing Label");

            when(tagRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(tagRepository.existsByLabelIgnoreCaseAndIdNot("Existing Label", 5L)).thenReturn(true);

            assertThatThrownBy(() -> tagService.updateTag(5L, request))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.TAG_LABEL_EXISTED);

            verify(tagRepository).findById(5L);
            verify(tagRepository).existsByLabelIgnoreCaseAndIdNot("Existing Label", 5L);
            verify(tagRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteTag")
    class DeleteTagTests {

        @Test
        @DisplayName("Delete tag successfully when tag exists and has no references")
        void deleteTag_Success() {
            Tag tag = Tag.builder().id(3L).label("Unused Tag").build();
            when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));

            tagService.deleteTag(3L);

            verify(tagRepository).findById(3L);
            verify(tagRepository).delete(tag);
            verify(tagRepository).flush();
        }

        @Test
        @DisplayName("Throw TAG_NOT_FOUND when tag to delete does not exist")
        void deleteTag_NotFound() {
            when(tagRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tagService.deleteTag(99L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.TAG_NOT_FOUND);

            verify(tagRepository).findById(99L);
            verify(tagRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Throw TAG_IN_USE when tag is referenced by decks or lessons")
        void deleteTag_ReferencedByDeckOrLesson_ThrowsTagInUse() {
            Tag tag = Tag.builder().id(1L).label("Referenced Tag").build();
            when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
            doThrow(new DataIntegrityViolationException("foreign key constraint fails"))
                    .when(tagRepository).flush();

            assertThatThrownBy(() -> tagService.deleteTag(1L))
                    .isInstanceOf(AppException.class)
                    .hasFieldOrPropertyWithValue("appError", AppError.TAG_IN_USE);

            verify(tagRepository).findById(1L);
            verify(tagRepository).delete(tag);
            verify(tagRepository).flush();
        }
    }
}
