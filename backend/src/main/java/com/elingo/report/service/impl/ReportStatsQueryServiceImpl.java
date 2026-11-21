package com.elingo.report.service.impl;

import com.elingo.report.repository.ReportRepository;
import com.elingo.report.service.ReportStatsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportStatsQueryServiceImpl implements ReportStatsQueryService {

    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReportRepository.StatusCountProjection> countReportsByStatus(
            LocalDateTime from, LocalDateTime to) {
        return reportRepository.countReportsByStatus(from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportRepository.TargetTypeCountProjection> countReportsByTargetType(
            LocalDateTime from, LocalDateTime to) {
        return reportRepository.countReportsByTargetType(from, to);
    }
}
