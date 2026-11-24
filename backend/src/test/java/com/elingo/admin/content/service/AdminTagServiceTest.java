package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.admin.content.service.impl.AdminTagServiceImpl;
import com.elingo.vocabulary.dto.response.TagResponse;
import com.elingo.vocabulary.service.TagService;
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
class AdminTagServiceTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private AdminTagServiceImpl adminTagService;

    @Nested
    @DisplayName("getAllTags")
    class GetAllTagsTests {

        @Test
        @DisplayName("Delegate getAllTags to TagService")
        void getAllTags_Success() {
            List<TagResponse> expected = List.of(
                    new TagResponse(1L, "Grammar"),
                    new TagResponse(2L, "Travel")
            );
            when(tagService.getAllTags()).thenReturn(expected);

            List<TagResponse> actual = adminTagService.getAllTags();

            assertThat(actual).isEqualTo(expected);
            verify(tagService).getAllTags();
        }
    }

    @Nested
    @DisplayName("getTagById")
    class GetTagByIdTests {

        @Test
        @DisplayName("Delegate getTagById to TagService")
        void getTagById_Success() {
            TagResponse expected = new TagResponse(1L, "Grammar");
            when(tagService.getTagById(1L)).thenReturn(expected);

            TagResponse actual = adminTagService.getTagById(1L);

            assertThat(actual).isEqualTo(expected);
            verify(tagService).getTagById(1L);
        }
    }

    @Nested
    @DisplayName("createTag")
    class CreateTagTests {

        @Test
        @DisplayName("Delegate createTag to TagService")
        void createTag_Success() {
            TagRequest request = new TagRequest("Technology");
            TagResponse expected = new TagResponse(10L, "Technology");
            when(tagService.createTag(request)).thenReturn(expected);

            TagResponse actual = adminTagService.createTag(request);

            assertThat(actual).isEqualTo(expected);
            verify(tagService).createTag(request);
        }
    }

    @Nested
    @DisplayName("updateTag")
    class UpdateTagTests {

        @Test
        @DisplayName("Delegate updateTag to TagService")
        void updateTag_Success() {
            TagRequest request = new TagRequest("New Tech");
            TagResponse expected = new TagResponse(10L, "New Tech");
            when(tagService.updateTag(10L, request)).thenReturn(expected);

            TagResponse actual = adminTagService.updateTag(10L, request);

            assertThat(actual).isEqualTo(expected);
            verify(tagService).updateTag(10L, request);
        }
    }

    @Nested
    @DisplayName("deleteTag")
    class DeleteTagTests {

        @Test
        @DisplayName("Delegate deleteTag to TagService")
        void deleteTag_Success() {
            adminTagService.deleteTag(5L);

            verify(tagService).deleteTag(5L);
        }
    }
}
