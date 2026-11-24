package com.elingo.report.service;

import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import com.elingo.report.repository.ReportRepository;
import com.elingo.report.service.impl.ReportStatsQueryServiceImpl;
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
class ReportStatsQueryServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportStatsQueryServiceImpl reportStatsQueryService;

    private final LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 11, 4, 0, 0);

    @Nested
    @DisplayName("countReportsByStatus")
    class CountReportsByStatusTests {

        @Test
        @DisplayName("Should delegate to reportRepository.countReportsByStatus")
        void countReportsByStatus_Success() {
            com.elingo.report.dto.projection.StatusCountProjection proj = mock(com.elingo.report.dto.projection.StatusCountProjection.class);
            when(proj.getStatus()).thenReturn(ReportStatus.PENDING);
            when(proj.getCountVal()).thenReturn(5L);
            when(reportRepository.countReportsByStatus(from, to)).thenReturn(List.of(proj));

            List<com.elingo.report.dto.projection.StatusCountProjection> result = reportStatsQueryService.countReportsByStatus(from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(ReportStatus.PENDING);
            assertThat(result.get(0).getCountVal()).isEqualTo(5L);
            verify(reportRepository).countReportsByStatus(from, to);
        }
    }

    @Nested
    @DisplayName("countReportsByTargetType")
    class CountReportsByTargetTypeTests {

        @Test
        @DisplayName("Should delegate to reportRepository.countReportsByTargetType")
        void countReportsByTargetType_Success() {
            com.elingo.report.dto.projection.TargetTypeCountProjection proj = mock(com.elingo.report.dto.projection.TargetTypeCountProjection.class);
            when(proj.getTargetType()).thenReturn(ReportTargetType.POST);
            when(proj.getCountVal()).thenReturn(12L);
            when(reportRepository.countReportsByTargetType(from, to)).thenReturn(List.of(proj));

            List<com.elingo.report.dto.projection.TargetTypeCountProjection> result = reportStatsQueryService.countReportsByTargetType(from, to);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTargetType()).isEqualTo(ReportTargetType.POST);
            assertThat(result.get(0).getCountVal()).isEqualTo(12L);
            verify(reportRepository).countReportsByTargetType(from, to);
        }
    }
}
