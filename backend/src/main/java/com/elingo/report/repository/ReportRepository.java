package com.elingo.report.repository;

import com.elingo.report.entity.Report;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId, ReportTargetType targetType, Long targetId, ReportStatus status);

    Optional<Report> findByIdAndReporterId(Long id, Long reporterId);

    Page<Report> findByReporterId(Long reporterId, Pageable pageable);
}
