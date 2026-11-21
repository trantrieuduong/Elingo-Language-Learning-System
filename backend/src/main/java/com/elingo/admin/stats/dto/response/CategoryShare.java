package com.elingo.admin.stats.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Distribution item with category, count and percentage share")
public record CategoryShare<T>(
        @Schema(description = "Category key")
        T category,
        @Schema(description = "Number of items in this category", example = "25")
        Long count,
        @Schema(description = "Percentage share over total items", example = "50.0")
        Double percentage
) {}
