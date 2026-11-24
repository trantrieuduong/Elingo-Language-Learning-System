package com.elingo.vocabulary.dto.request;

import com.elingo.common.annotation.AllowedMediaKind;
import com.elingo.common.annotation.BuildMediaKey;
import com.elingo.common.enums.MediaKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SystemDeckRequest(
        @NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "INVALID_REQUEST")
        String title,

        @Size(max = 1000, message = "INVALID_REQUEST")
        String description,

        @Size(max = 500, message = "INVALID_REQUEST")
        @AllowedMediaKind(MediaKind.IMAGE)
        @BuildMediaKey String coverImageUrl,

        @NotNull(message = "INVALID_REQUEST")
        Boolean isPremium,

        Set<Long> tagIds,

        Set<Long> cefrLevelIds
) {
    public SystemDeckRequest {
        title = title == null ? null : title.strip();
        description = (description == null || description.isBlank()) ? null : description.strip();
        coverImageUrl = (coverImageUrl == null || coverImageUrl.isBlank()) ? null : coverImageUrl.strip();
        tagIds = tagIds == null ? Set.of() : tagIds;
        cefrLevelIds = cefrLevelIds == null ? Set.of() : cefrLevelIds;
    }
}
