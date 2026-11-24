package com.elingo.report.service;

import com.elingo.report.dto.projection.StatusCountProjection;
import com.elingo.report.dto.projection.TargetTypeCountProjection;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportStatsQueryService {
    List<StatusCountProjection> countReportsByStatus(LocalDateTime from, LocalDateTime to);
    List<TargetTypeCountProjection> countReportsByTargetType(LocalDateTime from, LocalDateTime to);
}
