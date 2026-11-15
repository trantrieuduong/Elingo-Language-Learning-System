package com.elingo.report.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import com.elingo.report.entity.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateReportRequest(
        @NotNull(message = "INVALID_REQUEST")
        ReportTargetType targetType,
        @NotNull(message = "INVALID_REQUEST")
        @Positive(message = "INVALID_REQUEST")
        Long targetId,
        @NotBlank(message = "INVALID_REQUEST")
        @Size(min = 10, max = 1000, message = "INVALID_REQUEST")
        String description,
        @BuildMediaKey
        @Size(max = 5, message = "INVALID_REQUEST")
        List<@NotBlank @AllowedMediaKind(MediaKind.IMAGE) String> imageUrls
) {
    public CreateReportRequest {
        description = description == null ? null : description.strip();
        imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
    }
}
