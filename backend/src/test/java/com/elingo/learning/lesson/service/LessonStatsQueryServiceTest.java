package com.elingo.learning.lesson.service;

import com.elingo.learning.lesson.entity.LessonStatus;
import com.elingo.learning.lesson.repository.LessonRepository;
import com.elingo.learning.lesson.service.impl.LessonStatsQueryServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class LessonStatsQueryServiceTest {

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private LessonStatsQueryServiceImpl lessonStatsQueryService;

    @Nested
    @DisplayName("countLessonsByStatus")
    class CountLessonsByStatusTests {

        @Test
        @DisplayName("Should delegate to lessonRepository.countLessonsByStatus")
        void countLessonsByStatus_Success() {
            com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection proj = mock(com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection.class);
            when(proj.getStatus()).thenReturn(LessonStatus.PUBLISHED);
            when(proj.getCountVal()).thenReturn(20L);
            when(lessonRepository.countLessonsByStatus()).thenReturn(List.of(proj));

            List<com.elingo.learning.lesson.dto.projection.LessonStatusCountProjection> result = lessonStatsQueryService.countLessonsByStatus();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(LessonStatus.PUBLISHED);
            assertThat(result.get(0).getCountVal()).isEqualTo(20L);
            verify(lessonRepository).countLessonsByStatus();
        }
    }

    @Nested
    @DisplayName("countLessonsByPremium")
    class CountLessonsByPremiumTests {

        @Test
        @DisplayName("Should delegate to lessonRepository.countLessonsByPremium")
        void countLessonsByPremium_Success() {
            com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection proj = mock(com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection.class);
            when(proj.getIsPremium()).thenReturn(false);
            when(proj.getCountVal()).thenReturn(15L);
            when(lessonRepository.countLessonsByPremium()).thenReturn(List.of(proj));

            List<com.elingo.learning.lesson.dto.projection.LessonPremiumCountProjection> result = lessonStatsQueryService.countLessonsByPremium();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsPremium()).isFalse();
            assertThat(result.get(0).getCountVal()).isEqualTo(15L);
            verify(lessonRepository).countLessonsByPremium();
        }
    }

    @Nested
    @DisplayName("getTopLessonsByCompletion")
    class GetTopLessonsByCompletionTests {

        @Test
        @DisplayName("Should delegate to lessonRepository.findTopLessonsByCompletion with limit")
        void getTopLessonsByCompletion_Success() {
            com.elingo.learning.lesson.dto.projection.TopLessonProjection proj = mock(com.elingo.learning.lesson.dto.projection.TopLessonProjection.class);
            when(proj.getLessonId()).thenReturn(10L);
            when(lessonRepository.findTopLessonsByCompletion(10)).thenReturn(List.of(proj));

            List<com.elingo.learning.lesson.dto.projection.TopLessonProjection> result = lessonStatsQueryService.getTopLessonsByCompletion(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLessonId()).isEqualTo(10L);
            verify(lessonRepository).findTopLessonsByCompletion(10);
        }

        @Test
        @DisplayName("Should enforce minimum limit of 1 when limit <= 0")
        void getTopLessonsByCompletion_EnforcesMinimumLimit() {
            when(lessonRepository.findTopLessonsByCompletion(1)).thenReturn(List.of());

            List<com.elingo.learning.lesson.dto.projection.TopLessonProjection> result = lessonStatsQueryService.getTopLessonsByCompletion(-1);

            assertThat(result).isEmpty();
            verify(lessonRepository).findTopLessonsByCompletion(1);
        }
    }
}
