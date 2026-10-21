package com.elingo.vocabulary.dto.request;

import com.elingo.vocabulary.entity.OwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record DeckSearchRequest(
        @Size(max = 100, message = "INVALID_REQUEST")
        String q,
        @Size(max = 10, message = "INVALID_REQUEST")
        Set<@NotBlank(message = "INVALID_REQUEST")
        @Size(max = 100, message = "TAG_INVALID") String> tags,
        @Size(max = 6, message = "INVALID_REQUEST")
        Set<@Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$", message = "CEFR_LEVEL_CODE_INVALID") String> cefr,
        Boolean premium,
        OwnerType ownerType
) {
        public DeckSearchRequest {
                if (ownerType == null) ownerType = OwnerType.SYSTEM;
        }
}
