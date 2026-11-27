package com.elingo.report.dto.response;

import com.elingo.common.annotation.BuildMediaUrl;
import com.elingo.report.entity.ReportStatus;
import com.elingo.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response payload containing detailed report information for administration")
public record AdminReportResponse(
        @Schema(description = "Unique identifier of the report", example = "42")
        Long id,

        @Schema(description = "Type of the reported target", example = "POST")
        ReportTargetType targetType,

        @Schema(description = "Identifier of the reported target", example = "105")
        Long targetId,

        @Schema(description = "Detailed reason or description provided by the reporter", example = "Spam links in post content")
        String description,

        @Schema(description = "Current processing status of the report", example = "PENDING")
        ReportStatus status,

        @Schema(description = "Identifier of the user who submitted the report", example = "12")
        Long reporterId,

        @Schema(description = "Username of the user who submitted the report", example = "jane_doe")
        String reporterUsername,

        @Schema(description = "List of evidence image URLs attached to the report")
        @BuildMediaUrl List<String> imageUrls,

        @Schema(description = "Identifier of the administrator who resolved or rejected the report", example = "1")
        Long resolvedById,

        @Schema(description = "Username of the administrator who resolved or rejected the report", example = "admin_root")
        String resolvedByUsername,

        @Schema(description = "Timestamp when the report was resolved or rejected", example = "2026-03-15T10:20:00")
        LocalDateTime resolvedAt,

        @Schema(description = "Administrative note explaining the resolution or rejection", example = "Confirmed spam and removed post")
        String resolutionNote,

        @Schema(description = "Timestamp when the report was created", example = "2026-03-14T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the report was last updated", example = "2026-03-15T10:20:00")
        LocalDateTime updatedAt
) {
}
