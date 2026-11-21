package com.elingo.report.service;

import com.elingo.report.repository.ReportRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportStatsQueryService {
    List<ReportRepository.StatusCountProjection> countReportsByStatus(LocalDateTime from, LocalDateTime to);
    List<ReportRepository.TargetTypeCountProjection> countReportsByTargetType(LocalDateTime from, LocalDateTime to);
}
