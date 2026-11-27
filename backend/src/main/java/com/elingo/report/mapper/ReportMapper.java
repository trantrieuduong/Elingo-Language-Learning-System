package com.elingo.report.mapper;

import com.elingo.report.dto.response.AdminReportResponse;
import com.elingo.report.dto.response.ReportResponse;
import com.elingo.report.entity.Report;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReportMapper {

    @Mapping(target = "imageUrls", source = "imageUrls")
    ReportResponse toResponse(Report report, List<String> imageUrls);

    @Mapping(target = "reporterId", source = "report.reporter.id")
    @Mapping(target = "reporterUsername", source = "report.reporter.username")
    @Mapping(target = "resolvedById", source = "report.resolvedBy.id")
    @Mapping(target = "resolvedByUsername", source = "report.resolvedBy.username")
    @Mapping(target = "imageUrls", source = "imageUrls")
    AdminReportResponse toAdminResponse(Report report, List<String> imageUrls);
}
