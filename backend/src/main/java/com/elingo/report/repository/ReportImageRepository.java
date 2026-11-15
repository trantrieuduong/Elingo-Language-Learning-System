package com.elingo.report.repository;

import com.elingo.report.entity.ReportImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReportImageRepository extends JpaRepository<ReportImage, Long> {
    List<ReportImage> findByReportIdIn(Collection<Long> reportIds);

    List<ReportImage> findByReportId(Long reportId);
}
