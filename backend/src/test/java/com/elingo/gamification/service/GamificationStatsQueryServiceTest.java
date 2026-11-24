package com.elingo.gamification.service;

import com.elingo.gamification.repository.UserXpRepository;
import com.elingo.gamification.repository.XpEventRepository;
import com.elingo.gamification.service.impl.GamificationStatsQueryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class GamificationStatsQueryServiceTest {

    @Mock
    private XpEventRepository xpEventRepository;

    @Mock
    private UserXpRepository userXpRepository;

    @InjectMocks
    private GamificationStatsQueryServiceImpl gamificationStatsQueryService;

    private final LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 11, 3, 23, 59, 59);

    @Nested
    @DisplayName("countDauTimeSeries Tests")
    class CountDauTimeSeriesTests {

        @Test
        @DisplayName("Should delegate to xpEventRepository.countDauByDay")
        void countDauTimeSeries_Success() {
            com.elingo.gamification.dto.projection.DateCountProjection proj = mock(com.elingo.gamification.dto.projection.DateCountProjection.class);
            when(xpEventRepository.countDauByDay(from, to)).thenReturn(List.of(proj));

            List<com.elingo.gamification.dto.projection.DateCountProjection> result = gamificationStatsQueryService.countDauTimeSeries(from, to);

            assertThat(result).hasSize(1).contains(proj);
            verify(xpEventRepository).countDauByDay(from, to);
        }
    }

    @Nested
    @DisplayName("countDistinctActiveUsers Tests")
    class CountDistinctActiveUsersTests {

        @Test
        @DisplayName("Should delegate to xpEventRepository.countDistinctUsersBetween")
        void countDistinctActiveUsers_Success() {
            when(xpEventRepository.countDistinctUsersBetween(from, to)).thenReturn(42L);

            long count = gamificationStatsQueryService.countDistinctActiveUsers(from, to);

            assertThat(count).isEqualTo(42L);
            verify(xpEventRepository).countDistinctUsersBetween(from, to);
        }
    }

    @Nested
    @DisplayName("countUsersByLevel Tests")
    class CountUsersByLevelTests {

        @Test
        @DisplayName("Should delegate to userXpRepository.countUsersByLevel")
        void countUsersByLevel_Success() {
            com.elingo.gamification.dto.projection.LevelDistribution dist = mock(com.elingo.gamification.dto.projection.LevelDistribution.class);
            when(userXpRepository.countUsersByLevel()).thenReturn(List.of(dist));

            List<com.elingo.gamification.dto.projection.LevelDistribution> result = gamificationStatsQueryService.countUsersByLevel();

            assertThat(result).hasSize(1).contains(dist);
            verify(userXpRepository).countUsersByLevel();
        }
    }
}
