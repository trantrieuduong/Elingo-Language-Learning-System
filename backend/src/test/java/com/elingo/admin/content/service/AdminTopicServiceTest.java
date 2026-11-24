package com.elingo.admin.content.service;

import com.elingo.vocabulary.dto.request.CreateSystemTopicRequest;
import com.elingo.vocabulary.dto.request.ReorderItemRequest;
import com.elingo.vocabulary.dto.request.UpdateSystemTopicRequest;
import com.elingo.admin.content.service.impl.AdminTopicServiceImpl;
import com.elingo.vocabulary.dto.response.TopicResponse;
import com.elingo.vocabulary.service.TopicService;
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
class AdminTopicServiceTest {

    @Mock
    private TopicService topicService;

    @InjectMocks
    private AdminTopicServiceImpl adminTopicService;

    private TopicResponse createSampleTopicResponse(Long id, String name, int order) {
        return new TopicResponse(
                id,
                name,
                "slug-" + id,
                order,
                0,
                0
        );
    }

    @Nested
    @DisplayName("getTopicsByDeckId")
    class GetTopicsByDeckIdTests {

        @Test
        @DisplayName("Get topics by deck ID successfully delegating to topicService")
        void getTopicsByDeckId_Success() {
            List<TopicResponse> expected = List.of(
                    createSampleTopicResponse(1L, "Topic 1", 1),
                    createSampleTopicResponse(2L, "Topic 2", 2)
            );
            when(topicService.getTopicsByDeckIdForAdmin(10L)).thenReturn(expected);

            List<TopicResponse> actual = adminTopicService.getTopicsByDeckId(10L);

            assertThat(actual).isEqualTo(expected);
            verify(topicService).getTopicsByDeckIdForAdmin(10L);
        }
    }

    @Nested
    @DisplayName("createSystemTopic")
    class CreateSystemTopicTests {

        @Test
        @DisplayName("Create system topic successfully delegating to topicService")
        void createSystemTopic_Success() {
            CreateSystemTopicRequest request = new CreateSystemTopicRequest(10L, "New Topic");
            TopicResponse expected = createSampleTopicResponse(100L, "New Topic", 1);
            when(topicService.createSystemTopic(request)).thenReturn(expected);

            TopicResponse actual = adminTopicService.createSystemTopic(request);

            assertThat(actual).isEqualTo(expected);
            verify(topicService).createSystemTopic(request);
        }
    }

    @Nested
    @DisplayName("updateSystemTopic")
    class UpdateSystemTopicTests {

        @Test
        @DisplayName("Update system topic successfully delegating to topicService")
        void updateSystemTopic_Success() {
            UpdateSystemTopicRequest request = new UpdateSystemTopicRequest("Renamed Topic");
            TopicResponse expected = createSampleTopicResponse(100L, "Renamed Topic", 1);
            when(topicService.updateSystemTopic(100L, request)).thenReturn(expected);

            TopicResponse actual = adminTopicService.updateSystemTopic(100L, request);

            assertThat(actual).isEqualTo(expected);
            verify(topicService).updateSystemTopic(100L, request);
        }
    }

    @Nested
    @DisplayName("reorderSystemTopics")
    class ReorderSystemTopicsTests {

        @Test
        @DisplayName("Reorder system topics successfully delegating to topicService")
        void reorderSystemTopics_Success() {
            List<Long> orderedIds = List.of(3L, 1L, 2L);
            ReorderItemRequest request = new ReorderItemRequest(orderedIds);

            adminTopicService.reorderSystemTopics(10L, request);

            verify(topicService).reorderSystemTopics(10L, orderedIds);
        }
    }

    @Nested
    @DisplayName("deleteSystemTopic")
    class DeleteSystemTopicTests {

        @Test
        @DisplayName("Delete system topic successfully delegating to topicService")
        void deleteSystemTopic_Success() {
            adminTopicService.deleteSystemTopic(55L);

            verify(topicService).deleteSystemTopic(55L);
        }
    }
}
