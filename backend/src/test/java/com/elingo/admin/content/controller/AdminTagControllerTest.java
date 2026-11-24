package com.elingo.admin.content.controller;

import com.elingo.vocabulary.dto.request.TagRequest;
import com.elingo.admin.content.service.AdminTagService;
import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.vocabulary.dto.response.TagResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Tag("unit")
class AdminTagControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminTagService adminTagService;

    @InjectMocks
    private AdminTagController adminTagController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminTagController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getAllTags")
    class GetAllTagsTests {

        @Test
        @DisplayName("Get all tags successfully returns 200 and data list")
        void getAllTags_Success() throws Exception {
            List<TagResponse> tags = List.of(
                    new TagResponse(1L, "Grammar"),
                    new TagResponse(2L, "Travel")
            );
            when(adminTagService.getAllTags()).thenReturn(tags);

            mockMvc.perform(get("/admin/tags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].label").value("Grammar"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].label").value("Travel"));

            verify(adminTagService).getAllTags();
        }
    }

    @Nested
    @DisplayName("getTagById")
    class GetTagByIdTests {

        @Test
        @DisplayName("Get tag by ID successfully returns 200 and tag detail")
        void getTagById_Success() throws Exception {
            TagResponse tag = new TagResponse(1L, "Grammar");
            when(adminTagService.getTagById(1L)).thenReturn(tag);

            mockMvc.perform(get("/admin/tags/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.label").value("Grammar"));

            verify(adminTagService).getTagById(1L);
        }

        @Test
        @DisplayName("Get non-existent tag returns 404")
        void getTagById_NotFound() throws Exception {
            when(adminTagService.getTagById(99L)).thenThrow(new AppException(AppError.TAG_NOT_FOUND));

            mockMvc.perform(get("/admin/tags/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TAG_NOT_FOUND"));

            verify(adminTagService).getTagById(99L);
        }
    }

    @Nested
    @DisplayName("createTag")
    class CreateTagTests {

        @Test
        @DisplayName("Create tag successfully returns 200")
        void createTag_Success() throws Exception {
            TagRequest request = new TagRequest("Technology");
            TagResponse created = new TagResponse(10L, "Technology");
            when(adminTagService.createTag(any(TagRequest.class))).thenReturn(created);

            mockMvc.perform(post("/admin/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(10))
                    .andExpect(jsonPath("$.data.label").value("Technology"));

            verify(adminTagService).createTag(any(TagRequest.class));
        }

        @Test
        @DisplayName("Create tag with blank label returns 400")
        void createTag_BlankLabel_ReturnsBadRequest() throws Exception {
            TagRequest invalidRequest = new TagRequest("");

            mockMvc.perform(post("/admin/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(adminTagService, never()).createTag(any());
        }

        @Test
        @DisplayName("Create tag with existing label returns 400 with TAG_LABEL_EXISTED")
        void createTag_DuplicateLabel_ReturnsBadRequest() throws Exception {
            TagRequest request = new TagRequest("Technology");
            when(adminTagService.createTag(any(TagRequest.class)))
                    .thenThrow(new AppException(AppError.TAG_LABEL_EXISTED));

            mockMvc.perform(post("/admin/tags")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TAG_LABEL_EXISTED"));

            verify(adminTagService).createTag(any(TagRequest.class));
        }
    }

    @Nested
    @DisplayName("updateTag")
    class UpdateTagTests {

        @Test
        @DisplayName("Update tag successfully returns 200")
        void updateTag_Success() throws Exception {
            TagRequest request = new TagRequest("New Grammar");
            TagResponse updated = new TagResponse(1L, "New Grammar");
            when(adminTagService.updateTag(eq(1L), any(TagRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/admin/tags/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.label").value("New Grammar"));

            verify(adminTagService).updateTag(eq(1L), any(TagRequest.class));
        }

        @Test
        @DisplayName("Update non-existent tag returns 404")
        void updateTag_NotFound() throws Exception {
            TagRequest request = new TagRequest("Any Label");
            when(adminTagService.updateTag(eq(99L), any(TagRequest.class)))
                    .thenThrow(new AppException(AppError.TAG_NOT_FOUND));

            mockMvc.perform(put("/admin/tags/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TAG_NOT_FOUND"));

            verify(adminTagService).updateTag(eq(99L), any(TagRequest.class));
        }
    }

    @Nested
    @DisplayName("deleteTag")
    class DeleteTagTests {

        @Test
        @DisplayName("Delete tag successfully returns 200")
        void deleteTag_Success() throws Exception {
            doNothing().when(adminTagService).deleteTag(1L);

            mockMvc.perform(delete("/admin/tags/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(adminTagService).deleteTag(1L);
        }

        @Test
        @DisplayName("Delete tag referenced by deck or lesson returns 400 with TAG_IN_USE")
        void deleteTag_Referenced_ReturnsBadRequestWithTagInUse() throws Exception {
            doThrow(new AppException(AppError.TAG_IN_USE))
                    .when(adminTagService).deleteTag(1L);

            mockMvc.perform(delete("/admin/tags/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].code").value("TAG_IN_USE"));

            verify(adminTagService).deleteTag(1L);
        }
    }
}
