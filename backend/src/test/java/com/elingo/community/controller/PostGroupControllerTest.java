package com.elingo.community.controller;

import com.elingo.common.exception.GlobalExceptionHandler;
import com.elingo.community.dto.response.PostGroupResponse;
import com.elingo.community.service.PostGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PostGroupControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PostGroupService postGroupService;

    @InjectMocks
    private PostGroupController postGroupController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postGroupController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("getPostGroups")
    class GetPostGroupsTests {

        @Test
        @DisplayName("Get all post groups successfully")
        void getPostGroups_Success() throws Exception {
            List<PostGroupResponse> mockResponses = List.of(
                    new PostGroupResponse(1L, "English Basic", "Beginner study group"),
                    new PostGroupResponse(2L, "IELTS 7.0+", "Advanced IELTS preparation")
            );
            when(postGroupService.getPostGroups()).thenReturn(mockResponses);

            mockMvc.perform(get("/post-groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].groupName").value("English Basic"))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].groupName").value("IELTS 7.0+"));

            verify(postGroupService).getPostGroups();
        }

        @Test
        @DisplayName("Get all post groups successfully when empty")
        void getPostGroups_Success_Empty() throws Exception {
            when(postGroupService.getPostGroups()).thenReturn(List.of());

            mockMvc.perform(get("/post-groups"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(postGroupService).getPostGroups();
        }
    }
}

