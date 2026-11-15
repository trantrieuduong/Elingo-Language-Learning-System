package com.elingo.report.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;

import java.time.LocalDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        String description,
        ReportStatus status,
        @BuildMediaUrl List<String> imageUrls,
        String resolutionNote,
        LocalDateTime resolvedAt,
        LocalDateTime createdAt
) {
}
