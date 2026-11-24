package com.elingo.report.repository;

import com.elingo.report.dto.projection.StatusCountProjection;
import com.elingo.report.dto.projection.TargetTypeCountProjection;
import com.elingo.report.entity.Report;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterId, ReportTargetType targetType, Long targetId, ReportStatus status);

    Optional<Report> findByIdAndReporterId(Long id, Long reporterId);

    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    @Query("SELECT r.status AS status, COUNT(r.id) AS countVal FROM Report r WHERE r.createdAt >= :from AND r.createdAt < :to GROUP BY r.status")
    List<StatusCountProjection> countReportsByStatus(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT r.targetType AS targetType, COUNT(r.id) AS countVal FROM Report r WHERE r.createdAt >= :from AND r.createdAt < :to GROUP BY r.targetType")
    List<TargetTypeCountProjection> countReportsByTargetType(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
