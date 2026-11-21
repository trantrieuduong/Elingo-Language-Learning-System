package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Distribution breakdown item containing category identifier, count, and percentage share")
public record CategoryShare<T>(
        @Schema(description = "Category key identifier (e.g., status, owner type, label)", example = "PUBLISHED")
        T category,

        @Schema(description = "Number of items belonging to this category", example = "25")
        Long count,

        @Schema(description = "Percentage share over total items (0.0 to 100.0)", example = "50.0")
        Double percentage
) {}
